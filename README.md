# Backend - Real Time Chat (SpringBoot)

## Visão Geral

Backend do sistema de chat em tempo real desenvolvido em Java com Spring Boot.

## Tecnologias

- Java 17
- Spring Boot 3.3.2
- Spring WebSocket (STOMP)
- Maven

## Estrutura do Projeto

```
src/main/java/com/example/chat/
├── ChatApplication.java                    # Classe principal
├── controller/                             # Controladores
│   └── ChatController.java                 # Chat e WebSocket
├── service/                                # Serviços
│   ├── ChatService.java                    # Lógica do chat
│   └── PrivateChatNotificationService.java # Notificações
├── model/                                  # Modelos
│   ├── ChatMessage.java                    # Mensagens
│   ├── User.java                           # Usuários
│   └── PrivateConversationInfo.java        # Conversas privadas
└── config/                                 # Configurações
    ├── WebSocketConfig.java                # WebSocket
    ├── WebConfig.java                      # CORS
    └── WebSocketUserHandshakeHandler.java  # Handshake
```

## Funcionalidades

### Chat Público
- WebSocket com STOMP
- Mensagens em tempo real
- Indicador de digitação
- Usuários online
- Histórico de mensagens

### Chat Privado
- Mensagens privadas
- Conversas
- Contadores de não lidas
- Notificações

## Executar

### Pré-requisitos
- Java 17+
- Maven 3.6+

### Comandos
```bash
cd java-realtime-chat

# Compilar
mvn clean compile

# Executar
mvn spring-boot:run
```

### Configurações
```properties
# application.properties
server.port=8080
spring.websocket.max-text-message-size=128KB
spring.web.cors.allowed-origins=*
```

## Endpoints

### WebSocket
- `/app/chat.send` - Enviar mensagem
- `/app/chat.addUser` - Conectar usuário
- `/app/chat.typing` - Indicar digitação
- `/topic/public` - Mensagens públicas
- `/topic/users` - Lista de usuários

### REST
- `GET /api/chat/users` - Usuários online
- `GET /api/chat/stats` - Estatísticas

## Testes

```bash
# Testes unitários
mvn test

# Cobertura
mvn jacoco:report
```

## Troubleshooting

### Problemas Comuns
1. **Porta em uso**: Alterar `server.port` no `application.properties`
2. **CORS**: Verificar `WebConfig.java`
3. **WebSocket**: Confirmar endpoint `/ws`

## Licença

Projeto sob licença MIT.
