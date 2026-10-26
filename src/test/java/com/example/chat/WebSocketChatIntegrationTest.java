package com.example.chat;

import com.example.chat.model.ChatMessage;
import com.example.chat.security.GuestSessionService.Credentials;
import com.example.chat.service.ChatService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:websocket-integration;DB_CLOSE_DELAY=-1"
})
class WebSocketChatIntegrationTest {
    @LocalServerPort private int port;
    @Autowired private TestRestTemplate rest;
    @Autowired private ObjectMapper mapper;
    @SpyBean private ChatService chatService;
    private final List<StompSession> sessions = new ArrayList<>();
    private final List<WebSocketStompClient> clients = new ArrayList<>();

    @AfterEach
    void closeConnections() {
        sessions.stream().filter(StompSession::isConnected).forEach(StompSession::disconnect);
        clients.forEach(WebSocketStompClient::stop);
    }

    @Test
    void startsWithUnambiguousRoutesAndKeepsPrivateFeatureDisabled() {
        assertThat(rest.getForEntity("/api/chat/health", Map.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rest.getForObject("/api/chat/version", Map.class)).containsEntry("privateMessaging", false);
        assertThat(rest.getForEntity("/api/chat/private/conversation/alice/bob", String.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_IMPLEMENTED);
    }

    @Test
    void processesJoinBeforeAnImmediatelyFollowingMessageEvenWhenRegistrationIsSlow() throws Exception {
        String username = "slow_" + UUID.randomUUID().toString().substring(0, 8);
        Credentials credentials = rest.postForObject("/api/chat/session", Map.of("username", username), Credentials.class);
        CountDownLatch registering = new CountDownLatch(1);
        CountDownLatch continueRegistration = new CountDownLatch(1);
        doAnswer(invocation -> {
            registering.countDown();
            assertThat(continueRegistration.await(5, TimeUnit.SECONDS)).isTrue();
            return invocation.callRealMethod();
        }).when(chatService).addUser(eq(username), anyString(), anyString());

        StompSession session = connect("/ws-native", credentials.token());
        BlockingQueue<JsonNode> errors = subscribe(session, "/user/queue/error");
        BlockingQueue<JsonNode> events = subscribe(session, "/topic/public");
        send(session, "/app/chat.addUser", Map.of("type", "JOIN", "sender", username));
        assertThat(registering.await(5, TimeUnit.SECONDS)).isTrue();
        try {
            send(session, "/app/chat.send", Map.of("content", "immediate message"));
            assertThat(errors.poll(300, TimeUnit.MILLISECONDS)).isNull();
        } finally {
            continueRegistration.countDown();
        }
        assertThat(next(events).path("type").asText()).isEqualTo("JOIN");
        assertThat(next(events).path("content").asText()).isEqualTo("immediate message");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/ws-native", "/ws"})
    void authenticatedPublicChatAndReconnectWorkOverBothTransports(String endpoint) throws Exception {
        String username = "alice_" + UUID.randomUUID().toString().substring(0, 8);
        var reserved = rest.postForEntity("/api/chat/session", Map.of("username", username), Credentials.class);
        assertThat(reserved.getStatusCode()).isEqualTo(HttpStatus.OK);
        Credentials credentials = reserved.getBody();
        assertThat(credentials).isNotNull();
        assertThat(rest.postForEntity("/api/chat/session", Map.of("username", username), String.class).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);

        StompSession first = connect(endpoint, credentials.token());
        BlockingQueue<JsonNode> publicEvents = subscribe(first, "/topic/public");
        send(first, "/app/chat.addUser", Map.of("type", "JOIN", "sender", "spoofed"));
        assertThat(next(publicEvents).path("sender").asText()).isEqualTo(username);

        String content = "public message " + UUID.randomUUID();
        send(first, "/app/chat.send", Map.of("content", content, "sender", "spoofed",
                "type", "PRIVATE_MESSAGE", "id", "client-id", "isPrivate", true));
        JsonNode accepted = next(publicEvents);
        assertThat(accepted.path("type").asText()).isEqualTo(ChatMessage.MessageType.CHAT.name());
        assertThat(accepted.path("sender").asText()).isEqualTo(username);
        assertThat(accepted.path("id").asText()).isNotBlank().isNotEqualTo("client-id");
        assertThat(accepted.path("private").asBoolean()).isFalse();

        StompSession resumed = connect(endpoint, credentials.token());
        BlockingQueue<JsonNode> resumedEvents = subscribe(resumed, "/topic/public");
        BlockingQueue<JsonNode> history = subscribe(resumed, "/user/queue/history");
        send(resumed, "/app/chat.addUser", Map.of("type", "JOIN", "sender", username));
        assertThat(next(resumedEvents).path("type").asText()).isEqualTo("JOIN");
        assertThat(next(history).findValuesAsText("id")).contains(accepted.path("id").asText());

        first.disconnect();
        send(resumed, "/app/chat.send", Map.of("content", "after reconnect"));
        assertThat(next(resumedEvents).path("content").asText()).isEqualTo("after reconnect");
    }

    private StompSession connect(String endpoint, String token) throws Exception {
        var transport = new StandardWebSocketClient();
        WebSocketStompClient client = new WebSocketStompClient(endpoint.equals("/ws")
                ? new SockJsClient(List.of(new WebSocketTransport(transport))) : transport);
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(mapper);
        client.setMessageConverter(converter);
        clients.add(client);
        StompHeaders headers = new StompHeaders();
        headers.set("Authorization", "Bearer " + token);
        StompSession session = client.connectAsync("ws://localhost:" + port + endpoint,
                new WebSocketHttpHeaders(), headers, new StompSessionHandlerAdapter() {}).get(5, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    private BlockingQueue<JsonNode> subscribe(StompSession session, String destination) {
        BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override public Type getPayloadType(StompHeaders headers) { return JsonNode.class; }
            @Override public void handleFrame(StompHeaders headers, Object payload) {
                messages.add((JsonNode) payload);
            }
        });
        return messages;
    }

    private void send(StompSession session, String destination, Object message) throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.setDestination(destination);
        headers.setContentType(MediaType.APPLICATION_JSON);
        session.send(headers, message);
    }

    private JsonNode next(BlockingQueue<JsonNode> messages) throws InterruptedException {
        JsonNode message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).withFailMessage("Timed out waiting for STOMP message").isNotNull();
        return message;
    }
}
