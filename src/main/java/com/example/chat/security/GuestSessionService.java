package com.example.chat.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Short-lived guest identities for this single-node demonstration.
 * Not a replacement for persistent accounts, OAuth2, or a distributed session store.
 */
@Service
public class GuestSessionService {
    private static final Duration ACTIVE_LIFETIME = Duration.ofHours(12);
    private static final Duration RECONNECT_GRACE = Duration.ofMinutes(2);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, GuestSession> byUsername = new HashMap<>();
    private final Map<String, GuestSession> byToken = new HashMap<>();

    public record Credentials(String username, String token) {}

    private static final class GuestSession {
        final String username;
        final String token;
        Instant expiresAt;

        GuestSession(String username, String token, Instant expiresAt) {
            this.username = username;
            this.token = token;
            this.expiresAt = expiresAt;
        }
    }

    public synchronized Credentials createOrResume(String username, String existingToken) {
        cleanupExpired();
        if (username == null || !username.matches("[a-zA-Z0-9_-]{1,30}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid username");
        }

        GuestSession existing = byUsername.get(username);
        if (existing != null) {
            if (existingToken != null && existing.token.equals(existingToken)) {
                existing.expiresAt = Instant.now().plus(ACTIVE_LIFETIME);
                return new Credentials(username, existing.token);
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
        }

        byte[] rawToken = new byte[32];
        RANDOM.nextBytes(rawToken);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(rawToken);
        GuestSession session = new GuestSession(username, token, Instant.now().plus(ACTIVE_LIFETIME));
        byUsername.put(username, session);
        byToken.put(token, session);
        return new Credentials(username, token);
    }

    public synchronized String requireUsername(String authorization) {
        cleanupExpired();
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Guest token required");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        GuestSession session = byToken.get(token);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Guest session expired or invalid");
        }
        session.expiresAt = Instant.now().plus(ACTIVE_LIFETIME);
        return session.username;
    }

    public synchronized void suspend(String username) {
        GuestSession session = byUsername.get(username);
        if (session != null) {
            // Keep the identity reserved briefly so the same browser can reconnect.
            session.expiresAt = Instant.now().plus(RECONNECT_GRACE);
        }
    }

    public synchronized void revoke(String authorization) {
        String username = requireUsername(authorization);
        GuestSession session = byUsername.remove(username);
        if (session != null) byToken.remove(session.token);
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        byToken.entrySet().removeIf(entry -> {
            GuestSession session = entry.getValue();
            if (now.isBefore(session.expiresAt)) return false;
            byUsername.remove(session.username, session);
            return true;
        });
    }
}
