package com.example.chat.security;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.Set;

/** Authenticates STOMP CONNECT before allowing subscriptions or application sends. */
public class GuestStompAuthInterceptor implements ChannelInterceptor {
    private static final Set<String> PUBLIC_TOPICS = Set.of(
            "/topic/public", "/topic/users", "/topic/typing", "/topic/reactions", "/topic/user-status"
    );
    private final GuestSessionService sessions;

    public GuestStompAuthInterceptor(GuestSessionService sessions) {
        this.sessions = sessions;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) return message;

        StompCommand command = accessor.getCommand();
        if (command == StompCommand.CONNECT) {
            accessor.setUser(sessions.requirePrincipal(accessor.getFirstNativeHeader("Authorization")));
            return message;
        }

        if (command == StompCommand.SEND || command == StompCommand.SUBSCRIBE) {
            if (accessor.getUser() == null) {
                throw new MessagingException("Authentication required");
            }
            String destination = accessor.getDestination();
            if (destination == null) throw new MessagingException("Missing destination");
            if (command == StompCommand.SEND && !destination.startsWith("/app/chat.")) {
                throw new MessagingException("Destination not allowed");
            }
            if (command == StompCommand.SUBSCRIBE
                    && !PUBLIC_TOPICS.contains(destination)
                    && !destination.startsWith("/user/queue/")) {
                throw new MessagingException("Subscription not allowed");
            }
        }
        return message;
    }
}
