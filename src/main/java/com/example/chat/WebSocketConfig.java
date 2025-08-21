// WebSocketConfig.java
package com.example.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Lazy
    @Autowired
    private ChatService chatService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker
        config.enableSimpleBroker("/topic", "/queue");

        // Application destination prefix
        config.setApplicationDestinationPrefixes("/app");

        // User destination prefix for private messages
        config.setUserDestinationPrefix("/user");

        // Configure heartbeat (keep-alive)
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000});
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setHeartbeatTime(25000)  // 25 seconds
                .setDisconnectDelay(30000) // 30 seconds
                .setStreamBytesLimit(128 * 1024) // 128KB
                .setHttpMessageCacheSize(1000)
                .setSessionCookieNeeded(false);

        // Register endpoint without SockJS for modern browsers
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
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