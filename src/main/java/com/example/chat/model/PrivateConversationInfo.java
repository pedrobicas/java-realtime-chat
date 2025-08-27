package com.example.chat.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Classe para representar informações de conversas privadas
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrivateConversationInfo {
    
    private String conversationId;
    private String otherUser;
    private ChatMessage lastMessage;
    private int unreadCount;
    private LocalDateTime lastActivity;
    private boolean isActive;
    
    public PrivateConversationInfo() {
        this.lastActivity = LocalDateTime.now();
        this.isActive = false;
        this.unreadCount = 0;
    }
    
    public PrivateConversationInfo(String otherUser, ChatMessage lastMessage) {
        this();
        this.otherUser = otherUser;
        this.lastMessage = lastMessage;
        this.conversationId = generateConversationId(otherUser);
    }
    
    /**
     * Gerar ID único para a conversa
     */
    private String generateConversationId(String otherUser) {
        // Usar hash simples para gerar ID único
        return "private_" + Math.abs(otherUser.hashCode());
    }
    
    // Getters e Setters
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }
    
    public String getOtherUser() {
        return otherUser;
    }
    
    public void setOtherUser(String otherUser) {
        this.otherUser = otherUser;
        if (this.conversationId == null) {
            this.conversationId = generateConversationId(otherUser);
        }
    }
    
    public ChatMessage getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(ChatMessage lastMessage) {
        this.lastMessage = lastMessage;
        if (lastMessage != null) {
            this.lastActivity = lastMessage.getTimestamp();
        }
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime getLastActivity() {
        return lastActivity;
    }
    
    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    /**
     * Incrementar contador de mensagens não lidas
     */
    public void incrementUnreadCount() {
        this.unreadCount++;
    }
    
    /**
     * Marcar como lida (zerar contador)
     */
    public void markAsRead() {
        this.unreadCount = 0;
    }
    
    /**
     * Verificar se há mensagens não lidas
     */
    public boolean hasUnreadMessages() {
        return this.unreadCount > 0;
    }
    
    /**
     * Obter preview da última mensagem
     */
    public String getLastMessagePreview() {
        if (lastMessage == null || lastMessage.getContent() == null) {
            return "Nenhuma mensagem";
        }
        
        String content = lastMessage.getContent();
        if (content.length() <= 50) {
            return content;
        }
        
        return content.substring(0, 47) + "...";
    }
    
    /**
     * Obter tempo formatado da última atividade
     */
    public String getFormattedLastActivity() {
        if (lastActivity == null) {
            return "";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(lastActivity, now).toMinutes();
        
        if (minutes < 1) {
            return "Agora";
        } else if (minutes < 60) {
            return minutes + " min atrás";
        } else if (minutes < 1440) { // menos de 24 horas
            long hours = minutes / 60;
            return hours + "h atrás";
        } else {
            long days = minutes / 1440;
            return days + " dias atrás";
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrivateConversationInfo that = (PrivateConversationInfo) o;
        return Objects.equals(conversationId, that.conversationId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(conversationId);
    }
    
    @Override
    public String toString() {
        return String.format("PrivateConversationInfo{conversationId='%s', otherUser='%s', unreadCount=%d, isActive=%s}",
                conversationId, otherUser, unreadCount, isActive);
    }
}




