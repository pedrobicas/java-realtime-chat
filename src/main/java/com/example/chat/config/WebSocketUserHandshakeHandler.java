package com.example.chat.config;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/** Every WebSocket connection gets a private, unguessable routing identity.
 * The public nickname in handshake attributes is only a guest display name.
 */
public class WebSocketUserHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler handler,
                                      Map<String, Object> attributes) {
        String routingId = "guest-" + UUID.randomUUID();
        return () -> routingId;
    }
}
