package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import com.example.chat.repository.PublicMessageRepository;
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
        PublicMessageRepository repository = mock(PublicMessageRepository.class);
        ChatMessage publicMessage = new ChatMessage(ChatMessage.MessageType.CHAT, "public hello", "alice");
        when(repository.recent(100)).thenReturn(List.of(publicMessage));
        service = new ChatService(messaging, notifications, repository);
        service.addUser("alice", "s1", "alice-principal");
        service.addUser("bob", "s2", "bob-principal");
        service.sendPrivateMessage("private secret", "alice", "bob");
        service.addUser("charlie", "s3", "charlie-principal");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> messages = ArgumentCaptor.forClass(List.class);
        verify(messaging).convertAndSendToUser(eq("charlie-principal"), eq("/queue/history"), messages.capture());
        assertEquals(List.of(publicMessage), messages.getValue());
        assertTrue(messages.getValue().stream().noneMatch(msg -> "private secret".equals(msg.getContent())));
        assertTrue(messages.getValue().stream().noneMatch(msg -> msg.getType() == ChatMessage.MessageType.PRIVATE_MESSAGE));
    }
}
