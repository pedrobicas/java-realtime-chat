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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
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
    public void sendMessage(@Payload PublicMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        // Ignore any spoofed sender, type, id, color, avatar or timestamp from the browser.
        ChatMessage result = chatService.processMessage(
                new ChatMessage(ChatMessage.MessageType.CHAT,
                        request == null ? null : request.content(), null), sessionId);
        if (result.getType() == ChatMessage.MessageType.ERROR) {
            sendErrorToSession(headerAccessor, result);
        } else {
            messagingTemplate.convertAndSend("/topic/public", result);
        }
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Valid @Payload ChatMessage chatMessage, SimpMessageHeaderAccessor headerAccessor) {
        String username = chatMessage.getSender();
        // The requested nickname must match the handshake; it is NOT the STOMP Principal.
        Object requestedName = headerAccessor.getSessionAttributes() == null ? null
                : headerAccessor.getSessionAttributes().get("username");
        if (headerAccessor.getUser() == null ||
                username == null || !username.equals(requestedName)) {
            sendErrorToSession(headerAccessor, ChatMessage.createErrorMessage("Identidade de sessao invalida"));
            return;
        }
        ChatMessage result = chatService.addUser(username, headerAccessor.getSessionId(),
                headerAccessor.getUser().getName());
        if (result.getType() == ChatMessage.MessageType.ERROR) {
            sendErrorToSession(headerAccessor, result);
        } else {
            messagingTemplate.convertAndSend("/topic/public", result);
        }
    }

    public record PublicMessageRequest(String content) {}

    private void sendErrorToSession(SimpMessageHeaderAccessor accessor, ChatMessage error) {
        if (accessor.getUser() != null) {
            messagingTemplate.convertAndSendToUser(accessor.getUser().getName(), "/queue/error", error);
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
    public void sendPrivateMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        // V2 dev: nao existe identidade autenticada. Chat privado fica DESATIVADO.
        // Nunca reabilitar o fluxo legado sem implementar autorizacao por sessao/Principal.
        logger.debug("Tentativa de chat privado recusada (feature indisponivel)");
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

    // Endpoints privados legados permanecem explicitamente inacessiveis nesta etapa.
    @GetMapping("/api/chat/private/conversations/{username}")
    @ResponseBody
    public List<PrivateConversationInfo> getUserPrivateConversations(@PathVariable String username) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Private chat requires authentication");
    }

    @GetMapping("/api/chat/private/conversation/{user1}/{user2}")
    @ResponseBody
    public List<ChatMessage> getPrivateConversationHistory(@PathVariable String user1, @PathVariable String user2) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Private chat requires authentication");
    }

    @PostMapping("/api/chat/private/mark-read")
    @ResponseBody
    public Map<String, Object> markPrivateMessagesAsRead(@RequestBody Map<String, String> request) {
        throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Private chat requires authentication");
    }

    @GetMapping("/api/chat/history")
    @ResponseBody
    public List<ChatMessage> getPublicHistory(@RequestParam(defaultValue = "50") int limit) {
        return chatService.getRecentPublicMessages(limit);
    }

    @GetMapping("/api/chat/health")
    @ResponseBody
    public Map<String, String> getHealth() {
        return Map.of("status", "UP");
    }

    @GetMapping("/api/chat/version")
    @ResponseBody
    public Map<String, Object> getVersion() {
        return Map.of("version", "2.0.0-dev.2", "stage", "development", "publicHistory", true,
                "privateMessaging", false);
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




