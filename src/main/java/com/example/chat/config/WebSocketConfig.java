// WebSocketConfig.java
package com.example.chat.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.chat.service.ChatService;
import com.example.chat.model.ChatMessage;
import com.example.chat.security.GuestSessionService;
import com.example.chat.security.GuestStompAuthInterceptor;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Value("${chat.allowed-origins}")
    private String allowedOrigins;

    @Lazy
    @Autowired
    private ChatService chatService;

    @Lazy
    @Autowired
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Autowired
    private GuestSessionService guestSessionService;

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new GuestStompAuthInterceptor(guestSessionService), new ClientDestinationGuard());
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
        // SUBSCRIBE, JOIN and subsequent SEND frames depend on this order per connection.
        registry.setPreserveReceiveOrder(true);
        // Register WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins.split(","))
                .withSockJS()
                .setHeartbeatTime(25000)  // 25 seconds
                .setDisconnectDelay(30000) // 30 seconds
                .setStreamBytesLimit(128 * 1024) // 128KB
                .setHttpMessageCacheSize(1000)
                .setSessionCookieNeeded(false);

        // Register endpoint without SockJS for modern browsers
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns(allowedOrigins.split(","));
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
                guestSessionService.suspend(leaveMessage.getSender());
                // Publish to remaining clients (the disconnected session will not receive it).
                messagingTemplate.convertAndSend("/topic/public", leaveMessage);
                logger.info("Usuário desconectado: {}", leaveMessage.getSender());
            }

        } catch (Exception e) {
            logger.error("Erro ao processar desconexão WebSocket: {}", e.getMessage());
        }
    }
}




