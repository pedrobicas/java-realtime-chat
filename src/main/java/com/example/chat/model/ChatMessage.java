// ChatMessage.java
package com.example.chat.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatMessage {

    public enum MessageType {
        CHAT, JOIN, LEAVE, TYPING, ERROR, INFO, EMOJI_REACTION,
        FILE_UPLOAD, PRIVATE_MESSAGE, SYSTEM_ANNOUNCEMENT, USER_LIST_UPDATE
    }

    public enum MessageStatus {
        SENT, DELIVERED, READ, FAILED
    }

    @NotNull
    private String id;

    @NotNull
    private MessageType type;

    @Size(max = 1000, message = "Mensagem muito longa")
    private String content;

    @NotBlank(message = "Remetente é obrigatório")
    @Size(max = 50, message = "Nome do remetente muito longo")
    private String sender;

    private String recipient; // Para mensagens privadas

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String formattedTime;

    private Integer onlineCount;

    private MessageStatus status;

    private String avatar; // URL do avatar do usuário

    private String color; // Cor personalizada do usuário

    private boolean isPrivate;

    private String replyToMessageId; // Para responder mensagens

    private List<String> mentions; // @usuário

    private String fileUrl; // Para uploads de arquivo

    private String fileName;

    private Long fileSize;

    private String mimeType;

    // Reações emoji
    private String emoji;
    private String reactedMessageId;

    // Construtor padrão
    public ChatMessage() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENT;
    }

    // Construtor com parâmetros básicos
    public ChatMessage(MessageType type, String content, String sender) {
        this();
        this.type = type;
        this.content = content;
        this.sender = sender;
    }

    // Construtor para mensagem privada
    public ChatMessage(String content, String sender, String recipient) {
        this();
        this.type = MessageType.PRIVATE_MESSAGE;
        this.content = content;
        this.sender = sender;
        this.recipient = recipient;
        this.isPrivate = true;
    }

    // Factory methods para diferentes tipos de mensagem
    public static ChatMessage createJoinMessage(String username, String avatar, String color) {
        ChatMessage message = new ChatMessage(MessageType.JOIN, username + " entrou no chat", username);
        message.setAvatar(avatar);
        message.setColor(color);
        return message;
    }

    public static ChatMessage createLeaveMessage(String username) {
        return new ChatMessage(MessageType.LEAVE, username + " saiu do chat", username);
    }

    public static ChatMessage createTypingMessage(String username) {
        return new ChatMessage(MessageType.TYPING, null, username);
    }

    public static ChatMessage createErrorMessage(String errorContent) {
        return new ChatMessage(MessageType.ERROR, errorContent, "Sistema");
    }

    public static ChatMessage createSystemMessage(String content) {
        return new ChatMessage(MessageType.SYSTEM_ANNOUNCEMENT, content, "Sistema");
    }

    public static ChatMessage createFileUploadMessage(String sender, String fileName,
                                                      String fileUrl, Long fileSize, String mimeType) {
        ChatMessage message = new ChatMessage(MessageType.FILE_UPLOAD,
                "Arquivo enviado: " + fileName, sender);
        message.setFileName(fileName);
        message.setFileUrl(fileUrl);
        message.setFileSize(fileSize);
        message.setMimeType(mimeType);
        return message;
    }

    public static ChatMessage createEmojiReaction(String sender, String reactedMessageId, String emoji) {
        ChatMessage message = new ChatMessage(MessageType.EMOJI_REACTION, null, sender);
        message.setReactedMessageId(reactedMessageId);
        message.setEmoji(emoji);
        return message;
    }

    // Métodos utilitários
    public boolean isSystemMessage() {
        return type == MessageType.JOIN || type == MessageType.LEAVE ||
                type == MessageType.ERROR || type == MessageType.SYSTEM_ANNOUNCEMENT;
    }

    public boolean isUserMessage() {
        return type == MessageType.CHAT || type == MessageType.PRIVATE_MESSAGE;
    }

    public boolean hasFile() {
        return type == MessageType.FILE_UPLOAD && fileUrl != null;
    }

    public boolean isImageFile() {
        return mimeType != null && mimeType.startsWith("image/");
    }

    public boolean isVideoFile() {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public String getFormattedFileSize() {
        if (fileSize == null) return "";

        double bytes = fileSize.doubleValue();
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;

        while (bytes >= 1024 && unitIndex < units.length - 1) {
            bytes /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", bytes, units[unitIndex]);
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }

    public Integer getOnlineCount() { return onlineCount; }
    public void setOnlineCount(Integer onlineCount) { this.onlineCount = onlineCount; }

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public String getReplyToMessageId() { return replyToMessageId; }
    public void setReplyToMessageId(String replyToMessageId) { this.replyToMessageId = replyToMessageId; }

    public List<String> getMentions() { return mentions; }
    public void setMentions(List<String> mentions) { this.mentions = mentions; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getEmoji() { return emoji; }
    public void setEmoji(String emoji) { this.emoji = emoji; }

    public String getReactedMessageId() { return reactedMessageId; }
    public void setReactedMessageId(String reactedMessageId) { this.reactedMessageId = reactedMessageId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("ChatMessage{id='%s', type=%s, sender='%s', content='%s'}",
                id, type, sender, content);
    }
}




