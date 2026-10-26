package com.example.chat.controller;

import com.example.chat.model.ChatMessage;
import com.example.chat.security.GuestSessionService;
import com.example.chat.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class ChatControllerTest {
    @Test
    void joinUsesTokenIdentityAndTreatsRepeatedJoinAsNoOp() {
        var sessions = new GuestSessionService();
        var credentials = sessions.createOrResume("alice", null);
        var principal = sessions.requirePrincipal("Bearer " + credentials.token());
        var headers = SimpMessageHeaderAccessor.create();
        headers.setSessionId("s1");
        headers.setUser(principal);
        var service = mock(ChatService.class);
        var messaging = mock(SimpMessagingTemplate.class);
        var controller = new ChatController();
        ReflectionTestUtils.setField(controller, "chatService", service);
        ReflectionTestUtils.setField(controller, "messagingTemplate", messaging);

        controller.addUser(new ChatMessage(ChatMessage.MessageType.JOIN, null, "spoofed"), headers);

        verify(service).addUser("alice", "s1", principal.getName());
        verifyNoInteractions(messaging);
    }
}
