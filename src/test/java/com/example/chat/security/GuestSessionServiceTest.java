package com.example.chat.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class GuestSessionServiceTest {
    @Test
    void routingIdentitySurvivesReconnectButChangesAfterRevocation() {
        GuestSessionService service = new GuestSessionService();
        var credentials = service.createOrResume("alice", null);
        var principal = service.requirePrincipal("Bearer " + credentials.token());
        assertEquals("alice", principal.username());
        assertNotEquals("alice", principal.getName());
        service.createOrResume("alice", credentials.token());
        assertEquals(principal, service.requirePrincipal("Bearer " + credentials.token()));
        service.revoke("Bearer " + credentials.token());
        var next = service.createOrResume("alice", null);
        assertNotEquals(principal.getName(), service.requirePrincipal("Bearer " + next.token()).getName());
    }

    @Test
    void uniqueGuestTokenIsRequiredToResumeClaimedUsername() {
        GuestSessionService service = new GuestSessionService();
        var first = service.createOrResume("alice", null);
        assertEquals("alice", service.requireUsername("Bearer " + first.token()));
        assertThrows(ResponseStatusException.class, () -> service.createOrResume("alice", null));
        assertEquals(first.token(), service.createOrResume("alice", first.token()).token());
        assertThrows(ResponseStatusException.class, () -> service.requireUsername("Bearer fake-token"));
        service.revoke("Bearer " + first.token());
        assertNotEquals(first.token(), service.createOrResume("alice", null).token());
    }

    @Test
    void rejectsInvalidUsernamesAndMissingTokens() {
        GuestSessionService service = new GuestSessionService();
        assertThrows(ResponseStatusException.class, () -> service.createOrResume("name with spaces", null));
        assertThrows(ResponseStatusException.class, () -> service.requireUsername(null));
    }
}
