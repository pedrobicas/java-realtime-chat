package com.example.chat.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.example.chat.model.ChatMessage;

/**
 * Serviço para gerenciar notificações de chat privado
 */
@Service
public class PrivateChatNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PrivateChatNotificationService.class);
    
    private final SimpMessagingTemplate messagingTemplate;
    
    // Armazenar contadores de mensagens não lidas por usuário
    private final Map<String, Map<String, Integer>> unreadCounts = new ConcurrentHashMap<>();
    
    @Autowired
    public PrivateChatNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * Notificar nova mensagem privada
     */
    public void notifyNewPrivateMessage(String sender, String recipient, ChatMessage message) {
        try {
            // Incrementar contador de mensagens não lidas
            incrementUnreadCount(recipient, sender);
            
            // Enviar notificação para o destinatário
            Map<String, Object> notification = Map.of(
                "type", "NEW_PRIVATE_MESSAGE",
                "sender", sender,
                "message", message.getContent(),
                "timestamp", message.getTimestamp(),
                "unreadCount", getUnreadCount(recipient, sender)
            );
            
            messagingTemplate.convertAndSendToUser(recipient, "/queue/notifications", notification);
            
            // Enviar confirmação para o remetente
            Map<String, Object> confirmation = Map.of(
                "type", "MESSAGE_SENT",
                "recipient", recipient,
                "timestamp", LocalDateTime.now()
            );
            
            messagingTemplate.convertAndSendToUser(sender, "/queue/notifications", confirmation);
            
            logger.info("Notificação de mensagem privada enviada: {} -> {}", sender, recipient);
            
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação de mensagem privada: {}", e.getMessage());
        }
    }
    
    /**
     * Notificar que mensagens foram lidas
     */
    public void notifyMessagesRead(String reader, String sender) {
        try {
            // Zerar contador de mensagens não lidas
            resetUnreadCount(reader, sender);
            
            // Enviar confirmação de leitura para o remetente
            Map<String, Object> readReceipt = Map.of(
                "type", "MESSAGES_READ",
                "reader", reader,
                "timestamp", LocalDateTime.now()
            );
            
            messagingTemplate.convertAndSendToUser(sender, "/queue/read-receipts", readReceipt);
            
            logger.info("Confirmação de leitura enviada: {} -> {}", reader, sender);
            
        } catch (Exception e) {
            logger.error("Erro ao enviar confirmação de leitura: {}", e.getMessage());
        }
    }
    
    /**
     * Notificar usuário online/offline
     */
    public void notifyUserStatusChange(String username, String status) {
        try {
            Map<String, Object> statusNotification = Map.of(
                "type", "USER_STATUS_CHANGE",
                "username", username,
                "status", status,
                "timestamp", LocalDateTime.now()
            );
            
            // Broadcast para todos os usuários
            messagingTemplate.convertAndSend("/topic/user-status", statusNotification);
            
            logger.info("Notificação de mudança de status enviada: {} -> {}", username, status);
            
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação de status: {}", e.getMessage());
        }
    }
    
    /**
     * Notificar usuário digitando
     */
    public void notifyUserTyping(String sender, String recipient) {
        try {
            Map<String, Object> typingNotification = Map.of(
                "type", "USER_TYPING",
                "sender", sender,
                "timestamp", LocalDateTime.now()
            );
            
            messagingTemplate.convertAndSendToUser(recipient, "/queue/typing", typingNotification);
            
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação de digitação: {}", e.getMessage());
        }
    }
    
    /**
     * Notificar usuário parou de digitar
     */
    public void notifyUserStoppedTyping(String sender, String recipient) {
        try {
            Map<String, Object> stopTypingNotification = Map.of(
                "type", "USER_STOPPED_TYPING",
                "sender", sender,
                "timestamp", LocalDateTime.now()
            );
            
            messagingTemplate.convertAndSendToUser(recipient, "/queue/typing", stopTypingNotification);
            
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação de parada de digitação: {}", e.getMessage());
        }
    }
    
    /**
     * Incrementar contador de mensagens não lidas
     */
    private void incrementUnreadCount(String recipient, String sender) {
        unreadCounts.computeIfAbsent(recipient, k -> new ConcurrentHashMap<>());
        unreadCounts.get(recipient).merge(sender, 1, Integer::sum);
    }
    
    /**
     * Obter contador de mensagens não lidas
     */
    private int getUnreadCount(String recipient, String sender) {
        Map<String, Integer> userCounts = unreadCounts.get(recipient);
        if (userCounts != null) {
            return userCounts.getOrDefault(sender, 0);
        }
        return 0;
    }
    
    /**
     * Resetar contador de mensagens não lidas
     */
    private void resetUnreadCount(String reader, String sender) {
        Map<String, Integer> userCounts = unreadCounts.get(reader);
        if (userCounts != null) {
            userCounts.put(sender, 0);
        }
    }
    
    /**
     * Obter total de mensagens não lidas para um usuário
     */
    public int getTotalUnreadCount(String username) {
        Map<String, Integer> userCounts = unreadCounts.get(username);
        if (userCounts != null) {
            return userCounts.values().stream().mapToInt(Integer::intValue).sum();
        }
        return 0;
    }
    
    /**
     * Obter contadores de mensagens não lidas para um usuário
     */
    public Map<String, Integer> getUserUnreadCounts(String username) {
        return unreadCounts.getOrDefault(username, new ConcurrentHashMap<>());
    }
    
    /**
     * Limpar contadores quando usuário sai
     */
    public void clearUserCounts(String username) {
        unreadCounts.remove(username);
        
        // Remover de contadores de outros usuários
        unreadCounts.values().forEach(userCounts -> userCounts.remove(username));
        
        logger.info("Contadores de mensagens não lidas limpos para usuário: {}", username);
    }
}




