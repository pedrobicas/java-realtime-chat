package com.example.chat.repository;

import com.example.chat.model.ChatMessage;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Armazena APENAS mensagens da sala pública, sem identidade autenticada. */
@Repository
public class PublicMessageRepository {
    private final JdbcTemplate jdbc;

    public PublicMessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void save(ChatMessage message) {
        if (message.getType() != ChatMessage.MessageType.CHAT) {
            throw new IllegalArgumentException("Only public CHAT events are persisted");
        }
        jdbc.update("""
                INSERT INTO public_messages (id, sender, content, created_at, formatted_time, avatar, color)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, message.getId(), message.getSender(), message.getContent(),
                Timestamp.valueOf(message.getTimestamp()), message.getFormattedTime(),
                message.getAvatar(), message.getColor());
    }

    /** Mais recentes, retornadas em ordem cronologica para o cliente. */
    public List<ChatMessage> recent(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        List<ChatMessage> newestFirst = jdbc.query("""
                SELECT id, sender, content, created_at, formatted_time, avatar, color
                FROM public_messages ORDER BY seq DESC LIMIT ?
                """, (rs, rowNum) -> {
                    ChatMessage message = new ChatMessage(ChatMessage.MessageType.CHAT,
                            rs.getString("content"), rs.getString("sender"));
                    message.setId(rs.getString("id"));
                    message.setTimestamp(rs.getTimestamp("created_at").toLocalDateTime());
                    message.setFormattedTime(rs.getString("formatted_time"));
                    message.setAvatar(rs.getString("avatar"));
                    message.setColor(rs.getString("color"));
                    return message;
                }, safeLimit);
        Collections.reverse(newestFirst);
        return newestFirst;
    }
}
