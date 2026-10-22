package com.example.chat.repository;

import com.example.chat.model.ChatMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import(PublicMessageRepository.class)
class PublicMessageRepositoryTest {
    @Autowired private PublicMessageRepository repository;

    private ChatMessage publicMessage(String text) {
        return new ChatMessage(ChatMessage.MessageType.CHAT, text, "Tester");
    }

    @Test
    void savesAndReturnsChronologicalMessages() {
        repository.save(publicMessage("hello <script>alert(1)</script>"));
        repository.save(publicMessage("second"));
        var result = repository.recent(10);
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("hello <script>alert(1)</script>");
        assertThat(result.get(1).getContent()).isEqualTo("second");
    }

    @Test
    void limitsHistoryAndRejectsNonPublicEvents() {
        repository.save(publicMessage("first"));
        repository.save(publicMessage("last"));
        assertThat(repository.recent(1)).extracting(ChatMessage::getContent).containsExactly("last");
        assertThatThrownBy(() -> repository.save(
                new ChatMessage(ChatMessage.MessageType.PRIVATE_MESSAGE, "secret", "Tester")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
