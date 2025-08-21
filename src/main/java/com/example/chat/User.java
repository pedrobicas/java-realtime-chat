// User.java
package com.example.chat;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {

    public enum UserStatus {
        ONLINE, AWAY, BUSY, OFFLINE
    }

    public enum UserRole {
        USER, MODERATOR, ADMIN
    }

    private String username;
    private String sessionId;
    private String avatar;
    private String color;
    private UserStatus status;
    private UserRole role;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime joinTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActivity;

    private boolean isTyping;
    private String currentRoom;
    private List<String> blockedUsers;
    private int messageCount;
    private boolean isMuted;
    private LocalDateTime mutedUntil;

    // Array de cores predefinidas para avatares
    private static final String[] AVATAR_COLORS = {
            "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
            "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9",
            "#F8C471", "#82E0AA", "#F1948A", "#85C1E9", "#D7BDE2"
    };

    // Construtor padrão
    public User() {
        this.joinTime = LocalDateTime.now();
        this.lastActivity = LocalDateTime.now();
        this.status = UserStatus.ONLINE;
        this.role = UserRole.USER;
        this.messageCount = 0;
        this.color = generateRandomColor();
        this.avatar = generateAvatarUrl();
    }

    // Construtor com username
    public User(String username, String sessionId) {
        this();
        this.username = username;
        this.sessionId = sessionId;
    }

    // Gerar cor aleatória
    private String generateRandomColor() {
        Random random = new Random();
        return AVATAR_COLORS[random.nextInt(AVATAR_COLORS.length)];
    }

    // Gerar URL do avatar usando um serviço gratuito
    private String generateAvatarUrl() {
        return String.format("https://api.dicebear.com/7.x/initials/svg?seed=%s&backgroundColor=%s&color=ffffff",
                username != null ? username : "user",
                color != null ? color.substring(1) : "4ECDC4");
    }

    // Atualizar última atividade
    public void updateLastActivity() {
        this.lastActivity = LocalDateTime.now();
        if (this.status == UserStatus.AWAY) {
            this.status = UserStatus.ONLINE;
        }
    }

    // Incrementar contador de mensagens
    public void incrementMessageCount() {
        this.messageCount++;
        updateLastActivity();
    }

    // Verificar se o usuário está mutado
    public boolean isMuted() {
        if (mutedUntil == null) return this.isMuted;

        if (LocalDateTime.now().isAfter(mutedUntil)) {
            this.isMuted = false;
            this.mutedUntil = null;
            return false;
        }
        return true;
    }

    // Mutar usuário por tempo determinado
    public void muteUntil(LocalDateTime until) {
        this.isMuted = true;
        this.mutedUntil = until;
    }

    // Verificar se pode enviar mensagem
    public boolean canSendMessage() {
        return !isMuted() && status != UserStatus.OFFLINE;
    }

    // Verificar se é moderador ou admin
    public boolean hasModeratorRights() {
        return role == UserRole.MODERATOR || role == UserRole.ADMIN;
    }

    // Verificar se é admin
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    // Calcular tempo online em minutos
    public long getOnlineTimeMinutes() {
        return java.time.Duration.between(joinTime, LocalDateTime.now()).toMinutes();
    }

    // Verificar se está inativo (mais de 5 minutos sem atividade)
    public boolean isInactive() {
        return java.time.Duration.between(lastActivity, LocalDateTime.now()).toMinutes() > 5;
    }

    // Factory method para criar usuário admin
    public static User createAdmin(String username, String sessionId) {
        User admin = new User(username, sessionId);
        admin.setRole(UserRole.ADMIN);
        return admin;
    }

    // Factory method para criar moderador
    public static User createModerator(String username, String sessionId) {
        User moderator = new User(username, sessionId);
        moderator.setRole(UserRole.MODERATOR);
        return moderator;
    }

    // Getters e Setters
    public String getUsername() { return username; }
    public void setUsername(String username) {
        this.username = username;
        this.avatar = generateAvatarUrl(); // Regenerar avatar com novo nome
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public String getColor() { return color; }
    public void setColor(String color) {
        this.color = color;
        this.avatar = generateAvatarUrl(); // Regenerar avatar com nova cor
    }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public LocalDateTime getJoinTime() { return joinTime; }
    public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }

    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public boolean isTyping() { return isTyping; }
    public void setTyping(boolean typing) { isTyping = typing; }

    public String getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(String currentRoom) { this.currentRoom = currentRoom; }

    public List<String> getBlockedUsers() { return blockedUsers; }
    public void setBlockedUsers(List<String> blockedUsers) { this.blockedUsers = blockedUsers; }

    public int getMessageCount() { return messageCount; }
    public void setMessageCount(int messageCount) { this.messageCount = messageCount; }

    public void setMuted(boolean muted) { isMuted = muted; }

    public LocalDateTime getMutedUntil() { return mutedUntil; }
    public void setMutedUntil(LocalDateTime mutedUntil) { this.mutedUntil = mutedUntil; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return String.format("User{username='%s', status=%s, role=%s, messageCount=%d}",
                username, status, role, messageCount);
    }
}