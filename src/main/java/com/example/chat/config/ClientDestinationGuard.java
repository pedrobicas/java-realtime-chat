package com.example.chat.config;

import java.util.Set;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

/**
 * Guests can SEND only into application handlers, never straight into broker destinations.
 * This is not authentication; it just prevents client-generated /topic messages.
 */
public class ClientDestinationGuard implements ChannelInterceptor {
    private static final Set<String> SENDS = Set.of(
            "/app/chat.addUser", "/app/chat.send", "/app/chat.typing", "/app/chat.leave");
    private static final Set<String> SUBSCRIPTIONS = Set.of(
            "/topic/public", "/topic/users", "/topic/typing",
            "/user/queue/history", "/user/queue/mention", "/user/queue/error");

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (StompCommand.SEND == command && !SENDS.contains(accessor.getDestination())) {
            return null;
        }
        if (StompCommand.SUBSCRIBE == command && !SUBSCRIPTIONS.contains(accessor.getDestination())) {
            return null;
        }
        return message;
    }
}
