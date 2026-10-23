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
    private SimpMessagingTemplate messaging;

    @BeforeEach
    void setUp() {
        repository = mock(PublicMessageRepository.class);
        messaging = mock(SimpMessagingTemplate.class);
        service = new ChatService(messaging,
                mock(PrivateChatNotificationService.class), repository);
    }

    @AfterEach
    void tearDown() {
        service.shutdown();
    }

    @Test
    void rejectsInvalidDisplayName() {
        assertThat(service.addUser("nome com espaco", "s1", "guest-s1").getType())
                .isEqualTo(ChatMessage.MessageType.ERROR);
    }

    @Test
    void requiresConnectedSenderAndRejectsLongMessages() {
        ChatMessage message = new ChatMessage(ChatMessage.MessageType.CHAT, "Oi", "Invasor");
        assertThat(service.processMessage(message, "unknown").getType())
                .isEqualTo(ChatMessage.MessageType.ERROR);
        assertThat(service.addUser("Tester", "s1", "guest-s1").getType())
                .isEqualTo(ChatMessage.MessageType.JOIN);
        message.setContent("x".repeat(1001));
        assertThat(service.processMessage(message, "s1").getType())
                .isEqualTo(ChatMessage.MessageType.ERROR);
        verify(repository, never()).save(any());
    }

    @Test
    void historyIsRoutedToUniqueSessionPrincipalRatherThanNickname() {
        service.addUser("Ana", "s1", "guest-f34d");
        verify(messaging).convertAndSendToUser(
                eq("guest-f34d"), eq("/queue/history"), anyList());
        verify(messaging, never()).convertAndSendToUser(
                eq("Ana"), eq("/queue/history"), anyList());
    }

    @Test
    void duplicateNicknameIsRejectedAcrossSessions() {
        assertThat(service.addUser("Ana", "s1", "guest-one").getType())
                .isEqualTo(ChatMessage.MessageType.JOIN);
        assertThat(service.addUser("Ana", "s2", "guest-two").getType())
                .isEqualTo(ChatMessage.MessageType.ERROR);
        service.removeUser("s1");
        assertThat(service.addUser("Ana", "s2", "guest-two").getType())
                .isEqualTo(ChatMessage.MessageType.JOIN);
    }

    @Test
    void bindsAuthorToSessionAndPersistsBeforePublishing() {
        service.addUser("Tester", "s1", "guest-s1");
        ChatMessage message = new ChatMessage(ChatMessage.MessageType.CHAT, "  ola  ", "FakeUser");
        ChatMessage processed = service.processMessage(message, "s1");
        assertThat(processed.getType()).isEqualTo(ChatMessage.MessageType.CHAT);
        assertThat(processed.getSender()).isEqualTo("Tester");
        assertThat(processed.getContent()).isEqualTo("ola");
        assertThat(processed.getId()).isNotEqualTo(message.getId());
        verify(repository).save(argThat(saved ->
                saved != message && saved.getSender().equals("Tester")
                        && saved.getContent().equals("ola") && saved.getId() != null));
    }
}
