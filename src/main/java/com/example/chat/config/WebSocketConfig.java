// WebSocketConfig.java
package com.example.chat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import com.example.chat.service.ChatService;
import com.example.chat.model.ChatMessage;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Lazy
    @Autowired
    private ChatService chatService;

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(taskScheduler());

        // Application destination prefix
        config.setApplicationDestinationPrefixes("/app");

        // User destination prefix for private messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(userHandshakeInterceptor())
                .setHandshakeHandler(new WebSocketUserHandshakeHandler())
                .withSockJS()
                .setHeartbeatTime(25000)  // 25 seconds
                .setDisconnectDelay(30000) // 30 seconds
                .setStreamBytesLimit(128 * 1024) // 128KB
                .setHttpMessageCacheSize(1000)
                .setSessionCookieNeeded(false);

        // Register endpoint without SockJS for modern browsers
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*")
                .addInterceptors(userHandshakeInterceptor())
                .setHandshakeHandler(new WebSocketUserHandshakeHandler());
    }

    @Bean
    public HandshakeInterceptor userHandshakeInterceptor() {
        return new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request,
                                           ServerHttpResponse response,
                                           WebSocketHandler wsHandler,
                                           Map<String, Object> attributes) throws Exception {
                String username = null;
                try {
                    String query = request.getURI() != null ? request.getURI().getQuery() : null;
                    if (query != null) {
                        for (String pair : query.split("&")) {
                            int idx = pair.indexOf('=');
                            if (idx > 0) {
                                String key = pair.substring(0, idx);
                                String val = pair.substring(idx + 1);
                                if ("username".equals(key)) {
                                    username = java.net.URLDecoder.decode(val, java.nio.charset.StandardCharsets.UTF_8);
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {}

                if (username != null && !username.isBlank()) {
                    attributes.put("username", username);
                }
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request,
                                       ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Exception exception) {}
        };
    }

    /**
     * Handle WebSocket connection event
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();

            logger.info("Nova conexão WebSocket estabelecida. SessionId: {}", sessionId);

        } catch (Exception e) {
            logger.error("Erro ao processar conexão WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Handle WebSocket disconnect event
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();

            logger.info("Conexão WebSocket encerrada. SessionId: {}", sessionId);

            // Remove user from chat
            ChatMessage leaveMessage = chatService.removeUser(sessionId);

            // Broadcast leave message if user was found
            if (leaveMessage != null) {
                // Note: Cannot use messaging template here as the session is already closed
                // The chatService will handle broadcasting through other means
                logger.info("Usuário desconectado: {}", leaveMessage.getSender());
            }

        } catch (Exception e) {
            logger.error("Erro ao processar desconexão WebSocket: {}", e.getMessage());
        }
    }
}




