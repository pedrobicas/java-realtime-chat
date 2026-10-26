package com.example.chat.controller;

import com.example.chat.security.GuestSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat/session")
public class GuestSessionController {
    private final GuestSessionService sessions;

    public GuestSessionController(GuestSessionService sessions) {
        this.sessions = sessions;
    }

    public record StartSessionRequest(String username, String token) {}

    @PostMapping
    public GuestSessionService.Credentials start(@RequestBody StartSessionRequest request) {
        return sessions.createOrResume(request.username(), request.token());
    }

    @DeleteMapping
    public ResponseEntity<Void> end(@RequestHeader(value = "Authorization", required = false) String authorization) {
        sessions.revoke(authorization);
        return ResponseEntity.noContent().build();
    }
}
