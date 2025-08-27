// ChatController.java
package com.example.chat.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.example.chat.service.ChatService;
import com.example.chat.model.ChatMessage;
import com.example.chat.model.User;
import com.example.chat.model.PrivateConversationInfo;

@Controller
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    @Autowired
    private ChatService chatService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Processar mensagem de chat
     */
    @MessageMapping("/chat.send")
    @SendTo("/topic/public")
    public ChatMessage sendMessage(@Valid @Payload ChatMessage chatMessage,
                                   SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            return chatService.processMessage(chatMessage, sessionId);
        } catch (Exception e) {
            logger.error("Erro ao enviar mensagem: {}", e.getMessage());
            return ChatMessage.createErrorMessage("Erro ao enviar mensagem");
        }
    }

    /**
     * Adicionar usuário ao chat
     */
    @MessageMapping("/chat.addUser")
    @SendTo("/topic/public")
    public ChatMessage addUser(@Valid @Payload ChatMessage chatMessage,
                               SimpMessageHeaderAccessor headerAccessor) {
        try {
            String username = chatMessage.getSender();
            String sessionId = headerAccessor.getSessionId();

            // Adicionar username na sessão
            headerAccessor.getSessionAttributes().put("username", username);

            return chatService.addUser(username, sessionId);
        } catch (Exception e) {
            logger.error("Erro ao adicionar usuário: {}", e.getMessage());
            return ChatMessage.createErrorMessage("Erro ao conectar usuário");
        }
    }

    /**
     * Indicador de digitação
     */
    @MessageMapping("/chat.typing")
    public void typing(SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            chatService.processTyping(sessionId);
        } catch (Exception e) {
            logger.error("Erro ao processar typing: {}", e.getMessage());
        }
    }

    /**
     * Usuário deixa o chat
     */
    @MessageMapping("/chat.leave")
    @SendTo("/topic/public")
    public ChatMessage leaveUser(SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            return chatService.removeUser(sessionId);
        } catch (Exception e) {
            logger.error("Erro ao remover usuário: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Enviar mensagem privada
     */
    @MessageMapping("/chat.private")
    public void sendPrivateMessage(@Valid @Payload ChatMessage message,
                                   SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            User sender = chatService.getUserBySession(sessionId);

            if (sender != null && message.getRecipient() != null) {
                ChatMessage privateMessage = chatService.sendPrivateMessage(
                        message.getContent(),
                        sender.getUsername(),
                        message.getRecipient()
                );

                if (privateMessage.getType() == ChatMessage.MessageType.ERROR) {
                    messagingTemplate.convertAndSendToUser(
                            sender.getUsername(),
                            "/queue/error",
                            privateMessage
                    );
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao enviar mensagem privada: {}", e.getMessage());
        }
    }

    /**
     * Reagir com emoji
     */
    @MessageMapping("/chat.reaction")
    @SendTo("/topic/reactions")
    public ChatMessage addReaction(@Valid @Payload ChatMessage reaction,
                                   SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            User user = chatService.getUserBySession(sessionId);

            if (user != null) {
                reaction.setSender(user.getUsername());
                reaction.setType(ChatMessage.MessageType.EMOJI_REACTION);
                return reaction;
            }
            return null;
        } catch (Exception e) {
            logger.error("Erro ao processar reação: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Atualizar status do usuário
     */
    @MessageMapping("/chat.status")
    public void updateUserStatus(@RequestBody Map<String, String> statusUpdate,
                                 SimpMessageHeaderAccessor headerAccessor) {
        try {
            String sessionId = headerAccessor.getSessionId();
            User user = chatService.getUserBySession(sessionId);
            String newStatus = statusUpdate.get("status");

            if (user != null && newStatus != null) {
                try {
                    User.UserStatus status = User.UserStatus.valueOf(newStatus.toUpperCase());
                    user.setStatus(status);

                    // Broadcast da atualização
                    messagingTemplate.convertAndSend("/topic/users", chatService.getOnlineUsers());
                } catch (IllegalArgumentException e) {
                    logger.warn("Status inválido recebido: {}", newStatus);
                }
            }
        } catch (Exception e) {
            logger.error("Erro ao atualizar status: {}", e.getMessage());
        }
    }


    // Endpoints REST para informações adicionais

    /**
     * Obter usuários online (REST endpoint)
     */
    @GetMapping("/api/chat/users")
    @ResponseBody
    public List<User> getOnlineUsers() {
        return chatService.getOnlineUsers();
    }

    /**
     * Obter estatísticas do chat (REST endpoint)
     */
    @GetMapping("/api/chat/stats")
    @ResponseBody
    public Map<String, Object> getChatStats() {
        return chatService.getChatStats();
    }

    /**
     * Buscar usuário por username (REST endpoint)
     */
    @GetMapping("/api/chat/users/{username}")
    @ResponseBody
    public User getUserByUsername(@PathVariable String username) {
        return chatService.getUserByUsername(username);
    }

    // ===== ENDPOINTS PARA CHAT PRIVADO =====

    /**
     * Obter conversas privadas de um usuário (REST endpoint)
     */
    @GetMapping("/api/chat/private/conversations/{username}")
    @ResponseBody
    public List<PrivateConversationInfo> getUserPrivateConversations(@PathVariable String username) {
        try {
            return chatService.getUserPrivateConversations(username);
        } catch (Exception e) {
            logger.error("Erro ao obter conversas privadas para {}: {}", username, e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Obter histórico de conversa privada entre dois usuários (REST endpoint)
     */
    @GetMapping("/api/chat/private/conversation/{user1}/{user2}")
    @ResponseBody
    public List<ChatMessage> getPrivateConversationHistory(
            @PathVariable String user1,
            @PathVariable String user2) {
        try {
            return chatService.getPrivateConversationHistory(user1, user2);
        } catch (Exception e) {
            logger.error("Erro ao obter histórico de conversa privada: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Marcar mensagens privadas como lidas (REST endpoint)
     */
    @PostMapping("/api/chat/private/mark-read")
    @ResponseBody
    public Map<String, Object> markPrivateMessagesAsRead(
            @RequestBody Map<String, String> request) {
        try {
            String sender = request.get("sender");
            String recipient = request.get("recipient");
            
            if (sender != null && recipient != null) {
                chatService.markPrivateMessagesAsRead(sender, recipient);
                return Map.of("success", true, "message", "Mensagens marcadas como lidas");
            } else {
                return Map.of("success", false, "message", "Parâmetros inválidos");
            }
        } catch (Exception e) {
            logger.error("Erro ao marcar mensagens como lidas: {}", e.getMessage());
            return Map.of("success", false, "message", "Erro interno do servidor");
        }
    }

    /**
     * Verificar se usuário está online (REST endpoint)
     */
    @GetMapping("/api/chat/users/{username}/status")
    @ResponseBody
    public Map<String, Object> getUserStatus(@PathVariable String username) {
        try {
            boolean isOnline = chatService.isUserOnline(username);
            User.UserStatus status = chatService.getUserStatus(username);
            
            return Map.of(
                "username", username,
                "isOnline", isOnline,
                "status", status != null ? status.name() : "OFFLINE"
            );
        } catch (Exception e) {
            logger.error("Erro ao obter status do usuário {}: {}", username, e.getMessage());
            return Map.of("error", "Erro interno do servidor");
        }
    }

    /**
     * Buscar usuários por termo de busca (REST endpoint)
     */
    @GetMapping("/api/chat/users/search")
    @ResponseBody
    public List<User> searchUsers(@RequestParam String query) {
        try {
            String searchTerm = query.toLowerCase().trim();
            if (searchTerm.isEmpty()) {
                return chatService.getOnlineUsers();
            }
            
            return chatService.getOnlineUsers().stream()
                .filter(user -> user.getUsername().toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erro na busca de usuários: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/api/chat/health")
    @ResponseBody
    public Map<String, Object> healthCheck() {
        return Map.of(
                "status", "UP",
                "timestamp", java.time.LocalDateTime.now(),
                "activeUsers", chatService.getOnlineUsers().size()
        );
    }
}




