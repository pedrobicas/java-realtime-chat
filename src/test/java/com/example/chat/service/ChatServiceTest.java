package com.example.chat.service;

import com.example.chat.model.ChatMessage;
import com.example.chat.repository.PublicMessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatServiceTest {
    private ChatService service;
    private PublicMessageRepository repository;

    @BeforeEach
    void setUp() {
        repository = mock(PublicMessageRepository.class);
        service = new ChatService(mock(SimpMessagingTemplate.class),
                mock(PrivateChatNotificationService.class), repository);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void rejectsInvalidDisplayName() {
        assertThat(service.addUser("nome com espaco", "s1").getType())
                .isEqualTo(ChatMessage.MessageType.ERROR);
    }

    @Test
    void requiresConnectedSenderAndRejectsLongMessages() {
        ChatMessage message = new ChatMessage(ChatMessage.MessageType.CHAT, "Oi", "Invasor");
        assertThat(service.processMessage(message, "unknown").getType())
                .isEqualTo(ChatMessage.MessageType.ERROR);
        assertThat(service.addUser("Tester", "s1").getType())
                .isEqualTo(ChatMessage.MessageType.JOIN);
        message.setContent("x".repeat(1001));
        assertThat(service.processMessage(message, "s1").getType())
                .isEqualTo(ChatMessage.MessageType.ERROR);
        verify(repository, never()).save(any());
    }

    @Test
    void bindsAuthorToSessionAndPersistsBeforePublishing() {
        service.addUser("Tester", "s1");
        ChatMessage message = new ChatMessage(ChatMessage.MessageType.CHAT, "  ola  ", "FakeUser");
        ChatMessage processed = service.processMessage(message, "s1");
        assertThat(processed.getType()).isEqualTo(ChatMessage.MessageType.CHAT);
        assertThat(processed.getSender()).isEqualTo("Tester");
        assertThat(processed.getContent()).isEqualTo("ola");
        verify(repository).save(message);
    }
}
