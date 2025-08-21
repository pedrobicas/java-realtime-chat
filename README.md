# Java Realtime Chat (Spring Boot + STOMP/WebSocket)

Um projeto minimalista de **chat em tempo real** usando Spring Boot 3, STOMP sobre WebSocket e uma página HTML simples.

## Como rodar

1. **Pré‑requisitos**: Java 17+ e Maven 3.8+.
2. No terminal, dentro da pasta do projeto:
   ```bash
   mvn spring-boot:run
   ```

## Endpoints principais
- WebSocket endpoint: `/ws` (com SockJS opcional).
- STOMP:
  - Enviar mensagem: `/app/chat.send` (mensagens vão para `/topic/public`).
  - Entrar no chat: `/app/chat.addUser`.
  - Assinatura do feed público: `/topic/public`.

## Estrutura
```text
src/main/java/com/example/chat
├─ ChatApplication.java
├─ WebSocketConfig.java
├─ ChatMessage.java
└─ ChatController.java
src/main/resources
├─ application.properties
└─ templates/index.html
```

> Dica: Para publicar em produção com Nginx, faça proxy para `/ws` com suporte a WebSocket.

## Próximos passos (opcional)
- Persistir mensagens (PostgreSQL/MongoDB).
- Autenticação com JWT (Spring Security) e salas privadas.
- Escala horizontal com broker externo (RabbitMQ/Redis) via `enableStompBrokerRelay`.
- UI melhor com React/Vue/Angular usando a mesma API STOMP.
