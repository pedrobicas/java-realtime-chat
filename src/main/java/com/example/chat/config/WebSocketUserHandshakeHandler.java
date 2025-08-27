package com.example.chat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

public class WebSocketUserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Object usernameAttr = attributes.get("username");
        String principalName = usernameAttr != null ? String.valueOf(usernameAttr) : (String) attributes.get("SPRING.SESSION.ID");
        if (principalName == null || principalName.isBlank()) {
            principalName = "anon-" + UUID.randomUUID();
        }
        final String name = principalName;
        return () -> name;
    }
}




