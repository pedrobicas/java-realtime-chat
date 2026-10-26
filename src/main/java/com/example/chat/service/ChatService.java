// ChatService.java
package com.example.chat.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.example.chat.model.ChatMessage;
import com.example.chat.repository.PublicMessageRepository;
import com.example.chat.model.PrivateConversationInfo;
import com.example.chat.model.User;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MAX_MESSAGE_HISTORY = 100;
    private static final int TYPING_TIMEOUT_SECONDS = 3;

    private final SimpMessagingTemplate messagingTemplate;
    private final PrivateChatNotificationService notificationService;
    private final PublicMessageRepository publicMessages;
    private final Map<String, User> activeUsers = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPrincipal = new ConcurrentHashMap<>();
    private final List<ChatMessage> messageHistory = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, Set<String>> typingUsers = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastTypingTime = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Estatísticas do chat
    private volatile int totalMessages = 0;
    private volatile LocalDateTime chatStartTime = LocalDateTime.now();

    public ChatService(SimpMessagingTemplate messagingTemplate, PrivateChatNotificationService notificationService,
                       PublicMessageRepository publicMessages) {
        this.messagingTemplate = messagingTemplate;
        this.notificationService = notificationService;
        this.publicMessages = publicMessages;
        startBackgroundTasks();
    }

    @jakarta.annotation.PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    /**
     * Adicionar usuário ao chat
     */
    public ChatMessage addUser(String username, String sessionId, String routingPrincipal) {
        try {
<<<<<<< Updated upstream
            if (username == null || !username.matches("[A-Za-z0-9_-]{1,30}")) {
                return ChatMessage.createErrorMessage("Nome invalido. Use 1-30 caracteres: letras, numeros, _ e -");
            }
            if (sessionId == null || routingPrincipal == null || routingPrincipal.isBlank()) {
                return ChatMessage.createErrorMessage("Sessão inválida");
            }
            // Atomic registration: concurrent sessions cannot claim the same nickname.
            User user = new User(username, sessionId);
            if (activeUsers.putIfAbsent(username, user) != null) {
                return ChatMessage.createErrorMessage("Nome de usuário já está em uso");
=======
            // The STOMP principal is authenticated against the server-issued guest token.
            // A reconnect with that identity replaces the obsolete transport session.
            User user = new User(username, sessionId);
            User previous = activeUsers.put(username, user);
            if (previous != null) {
                if (sessionId.equals(previous.getSessionId())) return null;
                sessionToUsername.remove(previous.getSessionId(), username);
>>>>>>> Stashed changes
            }
            sessionToUsername.put(sessionId, username);
            sessionToPrincipal.put(sessionId, routingPrincipal);

            logger.info("Usuário {} conectado (sessão: {})", username, sessionId);

            // Criar mensagem de entrada
            ChatMessage joinMessage = ChatMessage.createJoinMessage(username, user.getAvatar(), user.getColor());
            joinMessage.setOnlineCount(activeUsers.size());
            joinMessage.setFormattedTime(LocalDateTime.now().format(TIME_FORMATTER));

            // Adicionar ao histórico
            addToHistory(joinMessage);

            // Enviar lista de usuários atualizada
            broadcastUserListUpdate();

            // Enviar histórico de mensagens para o novo usuário
            sendMessageHistoryToUser(routingPrincipal);

            // Notificar mudança de status
            notificationService.notifyUserStatusChange(username, "ONLINE");

            return joinMessage;

        } catch (Exception e) {
            logger.error("Erro ao adicionar usuário {}: {}", username, e.getMessage());
            return ChatMessage.createErrorMessage("Erro interno do servidor");
        }
    }

    /**
     * Remover usuário do chat
     */
    public ChatMessage removeUser(String sessionId) {
        try {
            String username = sessionToUsername.remove(sessionId);
            sessionToPrincipal.remove(sessionId);
            if (username == null) {
                return null;
            }

<<<<<<< Updated upstream
            User user = activeUsers.get(username);
            if (user == null || !sessionId.equals(user.getSessionId())
                    || !activeUsers.remove(username, user)) {
                return null;
            }
=======
            // Ignore delayed disconnects from an older session after reconnect.
            java.util.concurrent.atomic.AtomicReference<User> removed = new java.util.concurrent.atomic.AtomicReference<>();
            activeUsers.computeIfPresent(username, (key, current) -> {
                if (sessionId.equals(current.getSessionId())) {
                    removed.set(current);
                    return null;
                }
                return current;
            });
            User user = removed.get();
            if (user == null) return null;
>>>>>>> Stashed changes

            logger.info("Usuário {} desconectado (sessão: {})", username, sessionId);

            // Remover de usuários digitando
            removeFromTyping(username);

            // Criar mensagem de saída
            ChatMessage leaveMessage = ChatMessage.createLeaveMessage(username);
            leaveMessage.setOnlineCount(activeUsers.size());
            leaveMessage.setFormattedTime(LocalDateTime.now().format(TIME_FORMATTER));

            // Adicionar ao histórico
            addToHistory(leaveMessage);

            // Enviar lista de usuários atualizada
            broadcastUserListUpdate();

            // Notificar mudança de status e limpar contadores
            notificationService.notifyUserStatusChange(username, "OFFLINE");
            notificationService.clearUserCounts(username);

            return leaveMessage;

        } catch (Exception e) {
            logger.error("Erro ao remover usuário da sessão {}: {}", sessionId, e.getMessage());
            return null;
        }
    }

    /**
     * Processar mensagem de chat
     */
    public ChatMessage processMessage(ChatMessage message, String sessionId) {
        try {
            String username = sessionToUsername.get(sessionId);
            User user = activeUsers.get(username);

            if (user == null || !sessionId.equals(user.getSessionId())) {
                return ChatMessage.createErrorMessage("Usuário não encontrado na sessão");
            }

            // Verificar se usuário pode enviar mensagens
            if (!user.canSendMessage()) {
                String errorMsg = user.isMuted() ? "Você está mutado" : "Você não pode enviar mensagens";
                return ChatMessage.createErrorMessage(errorMsg);
            }

            // Validar mensagem
            if (message.getContent() == null || message.getContent().trim().isEmpty()) {
                return ChatMessage.createErrorMessage("Mensagem vazia");
            }
            String content = message.getContent().trim();
            if (content.length() > 1000) {
                return ChatMessage.createErrorMessage("Mensagem excede 1000 caracteres");
            }

            // Remover usuário da lista de digitando
            removeFromTyping(username);

<<<<<<< Updated upstream
            // Create a fresh server-owned message, discarding all client metadata.
            ChatMessage accepted = new ChatMessage(ChatMessage.MessageType.CHAT, content, username);
            accepted.setAvatar(user.getAvatar());
            accepted.setColor(user.getColor());
            accepted.setFormattedTime(accepted.getTimestamp().format(TIME_FORMATTER));
=======
            // The server, not the client, controls message identity and type.
            message.setId(UUID.randomUUID().toString());
            message.setType(ChatMessage.MessageType.CHAT);
            message.setSender(username);
            message.setTimestamp(LocalDateTime.now());
            message.setFormattedTime(LocalDateTime.now().format(TIME_FORMATTER));
            message.setAvatar(user.getAvatar());
            message.setColor(user.getColor());
            message.setStatus(ChatMessage.MessageStatus.SENT);
>>>>>>> Stashed changes

            // Persist BEFORE exposing the message to other clients.
            publicMessages.save(accepted);
            user.incrementMessageCount();
            totalMessages++;
            addToHistory(accepted);
            processMentions(accepted);
            logger.debug("Mensagem publica aceita de {}", username);
            return accepted;

        } catch (Exception e) {
            logger.error("Erro ao processar mensagem: {}", e.getMessage());
            return ChatMessage.createErrorMessage("Erro ao processar mensagem");
        }
    }

    /**
     * Processar indicador de digitação
     */
    public void processTyping(String sessionId) {
        try {
            String username = sessionToUsername.get(sessionId);
            if (username == null) return;

            User user = activeUsers.get(username);
            if (user == null || !sessionId.equals(user.getSessionId())) return;

            // Atualizar tempo de última digitação
            lastTypingTime.put(username, LocalDateTime.now());

            // Adicionar à lista de digitando se não estiver
            typingUsers.computeIfAbsent("public", k -> ConcurrentHashMap.newKeySet()).add(username);
            user.setTyping(true);

            // Enviar indicador de digitação
            ChatMessage typingMessage = ChatMessage.createTypingMessage(username);
            messagingTemplate.convertAndSend("/topic/typing", typingMessage);

        } catch (Exception e) {
            logger.error("Erro ao processar typing: {}", e.getMessage());
        }
    }

    /**
     * Enviar mensagem privada
     */
    public ChatMessage sendPrivateMessage(String content, String sender, String recipient) {
        try {
            User senderUser = activeUsers.get(sender);
            User recipientUser = activeUsers.get(recipient);

            if (senderUser == null || recipientUser == null) {
                return ChatMessage.createErrorMessage("Usuário não encontrado");
            }

            if (!senderUser.canSendMessage()) {
                return ChatMessage.createErrorMessage("Você não pode enviar mensagens");
            }

            // Criar mensagem privada
            ChatMessage privateMessage = new ChatMessage(content, sender, recipient);
            privateMessage.setTimestamp(LocalDateTime.now());
            privateMessage.setFormattedTime(LocalDateTime.now().format(TIME_FORMATTER));
            privateMessage.setAvatar(senderUser.getAvatar());
            privateMessage.setColor(senderUser.getColor());

            // Enviar para ambos os usuários usando destinos de usuário (requer Principal resolvido)
            messagingTemplate.convertAndSendToUser(sender, "/queue/private", privateMessage);
            messagingTemplate.convertAndSendToUser(recipient, "/queue/private", privateMessage);

            // Persistir no histórico para suportar listagem/recuperação
            addToHistory(privateMessage);

            // Enviar notificações
            notificationService.notifyNewPrivateMessage(sender, recipient, privateMessage);

            logger.info("Mensagem privada enviada: {} -> {}", sender, recipient);
            return privateMessage;

        } catch (Exception e) {
            logger.error("Erro ao enviar mensagem privada: {}", e.getMessage());
            return ChatMessage.createErrorMessage("Erro ao enviar mensagem privada");
        }
    }

    /**
     * Obter conversas privadas de um usuário
     */
    public List<PrivateConversationInfo> getUserPrivateConversations(String username) {
        try {
            List<PrivateConversationInfo> conversations = new ArrayList<>();
            
            // Buscar todas as mensagens privadas do usuário
            for (ChatMessage message : snapshotHistory()) {
                if (message.getType() == ChatMessage.MessageType.PRIVATE_MESSAGE && 
                    (message.getSender().equals(username) || message.getRecipient().equals(username))) {
                    
                    String otherUser = message.getSender().equals(username) ? 
                        message.getRecipient() : message.getSender();
                    
                    // Verificar se já existe conversa com este usuário
                    PrivateConversationInfo existingConversation = conversations.stream()
                        .filter(conv -> conv.getOtherUser().equals(otherUser))
                        .findFirst()
                        .orElse(null);
                    
                    if (existingConversation == null) {
                        // Criar nova conversa
                        existingConversation = new PrivateConversationInfo();
                        existingConversation.setOtherUser(otherUser);
                        existingConversation.setLastMessage(message);
                        existingConversation.setUnreadCount(0);
                        conversations.add(existingConversation);
                    } else {
                        // Atualizar última mensagem
                        if (message.getTimestamp().isAfter(existingConversation.getLastMessage().getTimestamp())) {
                            existingConversation.setLastMessage(message);
                        }
                    }
                }
            }
            
            // Ordenar por última mensagem (mais recente primeiro)
            conversations.sort((a, b) -> b.getLastMessage().getTimestamp().compareTo(a.getLastMessage().getTimestamp()));
            
            return conversations;
            
        } catch (Exception e) {
            logger.error("Erro ao obter conversas privadas para {}: {}", username, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obter histórico de mensagens privadas entre dois usuários
     */
    public List<ChatMessage> getPrivateConversationHistory(String user1, String user2) {
        try {
            List<ChatMessage> privateMessages = snapshotHistory().stream()
                .filter(message -> message.getType() == ChatMessage.MessageType.PRIVATE_MESSAGE)
                .filter(message -> 
                    (message.getSender().equals(user1) && message.getRecipient().equals(user2)) ||
                    (message.getSender().equals(user2) && message.getRecipient().equals(user1))
                )
                .sorted(Comparator.comparing(ChatMessage::getTimestamp))
                .collect(Collectors.toList());
            
            logger.info("Histórico de conversa privada obtido: {} mensagens entre {} e {}", 
                privateMessages.size(), user1, user2);
            
            return privateMessages;
            
        } catch (Exception e) {
            logger.error("Erro ao obter histórico de conversa privada: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Marcar mensagens privadas como lidas
     */
    public void markPrivateMessagesAsRead(String sender, String recipient) {
        try {
            // Enviar confirmação de leitura para o remetente
            ChatMessage readReceipt = ChatMessage.createSystemMessage("Mensagem lida");
            readReceipt.setType(ChatMessage.MessageType.INFO);
            readReceipt.setRecipient(sender);
            
            messagingTemplate.convertAndSendToUser(recipient, "/queue/read-receipt", readReceipt);
            
            // Notificar através do serviço de notificações
            notificationService.notifyMessagesRead(recipient, sender);
            
            logger.info("Mensagens marcadas como lidas: {} -> {}", recipient, sender);
            
        } catch (Exception e) {
            logger.error("Erro ao marcar mensagens como lidas: {}", e.getMessage());
        }
    }

    /**
     * Verificar se usuário está online
     */
    public boolean isUserOnline(String username) {
        return activeUsers.containsKey(username);
    }

    /**
     * Obter status de usuário
     */
    public User.UserStatus getUserStatus(String username) {
        User user = activeUsers.get(username);
        return user != null ? user.getStatus() : User.UserStatus.OFFLINE;
    }

    /**
     * Obter usuários online
     */
    public List<User> getOnlineUsers() {
        return new ArrayList<>(activeUsers.values());
    }

    /**
     * Obter estatísticas do chat
     */
    public Map<String, Object> getChatStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", activeUsers.size());
        stats.put("totalMessages", totalMessages);
        stats.put("chatUptime", java.time.Duration.between(chatStartTime, LocalDateTime.now()).toMinutes());
        stats.put("historySize", messageHistory.size());

        // Top usuários por mensagens
        List<Map<String, Object>> topUsers = activeUsers.values().stream()
                .sorted((a, b) -> Integer.compare(b.getMessageCount(), a.getMessageCount()))
                .limit(5)
                .map(user -> {
                    Map<String, Object> userStats = new HashMap<>();
                    userStats.put("username", user.getUsername());
                    userStats.put("messageCount", user.getMessageCount());
                    userStats.put("onlineTime", user.getOnlineTimeMinutes());
                    return userStats;
                })
                .collect(Collectors.toList());

        stats.put("topUsers", topUsers);
        return stats;
    }

    /**
     * Buscar usuário por username
     */
    public User getUserByUsername(String username) {
        return activeUsers.get(username);
    }

    /**
     * Buscar usuário por sessão
     */
    public User getUserBySession(String sessionId) {
        String username = sessionToUsername.get(sessionId);
        User user = username != null ? activeUsers.get(username) : null;
        return user != null && sessionId.equals(user.getSessionId()) ? user : null;
    }

    public List<ChatMessage> getRecentPublicMessages(int limit) {
        return publicMessages.recent(limit);
    }

    // Métodos privados
    private void addToHistory(ChatMessage message) {
        synchronized (messageHistory) {
            messageHistory.add(message);
            // Manter apenas as últimas mensagens
            while (messageHistory.size() > MAX_MESSAGE_HISTORY) {
                messageHistory.remove(0);
            }
        }
    }

<<<<<<< Updated upstream
    private void sendMessageHistoryToUser(String routingPrincipal) {
        try {
            List<ChatMessage> recentMessages = publicMessages.recent(MAX_MESSAGE_HISTORY);
            messagingTemplate.convertAndSendToUser(routingPrincipal, "/queue/history", recentMessages);
=======
    private List<ChatMessage> snapshotHistory() {
        synchronized (messageHistory) {
            return new ArrayList<>(messageHistory);
        }
    }

    private void sendMessageHistoryToUser(String username) {
        try {
            // Never deliver private conversation content through public chat history.
            List<ChatMessage> recentMessages = snapshotHistory().stream()
                    .filter(message -> message.getType() != ChatMessage.MessageType.PRIVATE_MESSAGE)
                    .filter(message -> !message.isPrivate())
                    .collect(Collectors.toList());
            messagingTemplate.convertAndSendToUser(username, "/queue/history", recentMessages);
>>>>>>> Stashed changes
        } catch (Exception e) {
            logger.error("Erro ao enviar histórico: {}", e.getMessage());
        }
    }

    private void broadcastUserListUpdate() {
        try {
            ChatMessage userListMessage = new ChatMessage();
            userListMessage.setType(ChatMessage.MessageType.USER_LIST_UPDATE);
            userListMessage.setOnlineCount(activeUsers.size());

            List<User> users = getOnlineUsers();
            messagingTemplate.convertAndSend("/topic/users", users);
        } catch (Exception e) {
            logger.error("Erro ao enviar lista de usuários: {}", e.getMessage());
        }
    }

    private void processMentions(ChatMessage message) {
        if (message.getContent() == null) return;

        List<String> mentions = new ArrayList<>();
        String[] words = message.getContent().split("\\s+");

        for (String word : words) {
            if (word.startsWith("@") && word.length() > 1) {
                String username = word.substring(1);
                if (activeUsers.containsKey(username)) {
                    mentions.add(username);
                }
            }
        }

        if (!mentions.isEmpty()) {
            message.setMentions(mentions);
            // Enviar notificação para usuários mencionados
            for (String username : mentions) {
                User mentioned = activeUsers.get(username);
                String principal = mentioned == null ? null : sessionToPrincipal.get(mentioned.getSessionId());
                if (principal != null) {
                    messagingTemplate.convertAndSendToUser(principal, "/queue/mention", message);
                }
            }
        }
    }

    private void removeFromTyping(String username) {
        typingUsers.values().forEach(set -> set.remove(username));
        lastTypingTime.remove(username);

        User user = activeUsers.get(username);
        if (user != null) {
            user.setTyping(false);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void startBackgroundTasks() {
        // Task para limpar usuários que pararam de digitar
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LocalDateTime cutoff = LocalDateTime.now().minusSeconds(TYPING_TIMEOUT_SECONDS);

                lastTypingTime.entrySet().removeIf(entry -> {
                    if (entry.getValue().isBefore(cutoff)) {
                        removeFromTyping(entry.getKey());
                        return true;
                    }
                    return false;
                });
            } catch (Exception e) {
                logger.error("Erro na limpeza de typing: {}", e.getMessage());
            }
        }, TYPING_TIMEOUT_SECONDS, TYPING_TIMEOUT_SECONDS, TimeUnit.SECONDS);

        // Task para atualizar status de usuários inativos
        scheduler.scheduleAtFixedRate(() -> {
            try {
                activeUsers.values().forEach(user -> {
                    if (user.isInactive() && user.getStatus() == User.UserStatus.ONLINE) {
                        user.setStatus(User.UserStatus.AWAY);
                        broadcastUserListUpdate();
                    }
                });
            } catch (Exception e) {
                logger.error("Erro na verificação de inatividade: {}", e.getMessage());
            }
        }, 5, 5, TimeUnit.MINUTES);
    }
}




