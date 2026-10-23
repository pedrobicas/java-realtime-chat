package com.example.chat.config;

import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class WebSocketUserHandshakeHandlerTest {
    @Test
    void usesUniqueRoutingPrincipalsNotDisplayNames() {
        var handler = new WebSocketUserHandshakeHandler();
        var attributes = Map.<String, Object>of("username", "Ana");
        var first = handler.determineUser(null, null, attributes);
        var second = handler.determineUser(null, null, attributes);

        assertThat(first).isNotNull();
        assertThat(second).isNotNull();
        assertThat(first.getName()).startsWith("guest-");
        assertThat(second.getName()).isNotEqualTo(first.getName());
        assertThat(first.getName()).isNotEqualTo("Ana");
    }
}
