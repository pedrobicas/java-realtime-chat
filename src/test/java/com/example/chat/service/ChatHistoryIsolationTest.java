package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatHistoryIsolationTest {
    private ChatService service;

    @AfterEach
    void tearDown() {
        if (service != null) service.shutdown();
    }

    @Test
    void joiningUserOnlyReceivesPublicHistory() {
        SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
        PrivateChatNotificationService notifications = mock(PrivateChatNotificationService.class);
        service = new ChatService(messaging, notifications);
        service.addUser("alice", "s1");
        service.addUser("bob", "s2");
        service.sendPrivateMessage("private secret", "alice", "bob");
        service.addUser("charlie", "s3");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> messages = ArgumentCaptor.forClass(List.class);
        verify(messaging).convertAndSendToUser(eq("charlie"), eq("/queue/history"), messages.capture());
        assertTrue(messages.getValue().stream().noneMatch(msg -> "private secret".equals(msg.getContent())));
        assertTrue(messages.getValue().stream().noneMatch(msg -> msg.getType() == ChatMessage.MessageType.PRIVATE_MESSAGE));
    }
}
