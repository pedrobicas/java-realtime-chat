# Realtime Chat v2 — Spring Boot API

Backend Java / Spring Boot para o [frontend Angular](https://github.com/pedrobicas/web-realtime-chat). Branch de desenvolvimento: `develop/v2`.

**Estado:** `2.0.0-dev.2` (prévia local; não publicar diretamente na Internet). O chat público usa STOMP sobre WebSocket, persiste mensagens no banco e restabelece o histórico em novas sessões. Conversas privadas continuam desligadas até autenticação e autorização completas.

## Execução local

Requisitos: **JDK 21** e **Maven 3.9+**.

```sh
mvn spring-boot:run
```

<<<<<<< Updated upstream
Por padrão: `http://localhost:8081`; banco H2 persistido em `./realtimechat-dev.mv.db` (arquivo ignorado pelo Git).

Para executar o **sistema completo** (PostgreSQL, backend e Angular via Nginx) clone ambos os repositórios como pastas irmãs e execute `docker compose up --build` na pasta `web-realtime-chat`. Veja as instruções em [`web-realtime-chat/README.md`](https://github.com/pedrobicas/web-realtime-chat/blob/develop/v2/README.md).

## Verificação

```sh
mvn verify
=======
### Configurações
```properties
# application.properties
server.port=8081
spring.websocket.max-text-message-size=128KB
spring.web.cors.allowed-origins=*
>>>>>>> Stashed changes
```

O CI executa compilação, testes e empacotamento no Java 21. Endpoints úteis:

- `GET /api/chat/history?limit=50` — últimas mensagens públicas;
- `GET /api/chat/version` — versão e recursos;
- `GET /api/chat/users` — usuários online.
- `GET /api/chat/health` — sinal básico de vida (não verifica a saúde do banco).

## Arquitetura / segurança

- **Mensagens públicas**: o backend cria IDs, timestamps, remetente e metadados; não confia nesses campos fornecidos pelo navegador.
- **Identidade temporária**: cada sessão WebSocket tem um `Principal` de roteamento único, distinto do apelido público. O apelido continua **sem autenticação**.
- **Persistência**: JDBC + H2 para desenvolvimento; PostgreSQL configurável com `DB_URL`, `DB_USER`, `DB_PASSWORD`, `DB_DRIVER`.
- **CORS e origem do WebSocket**: `CHAT_ALLOWED_ORIGINS` deve conter apenas origens de confiança.
- **Acesso de escrita ao broker**: clientes só podem enviar mensagens aos destinos `/app` autorizados.
- **Limites**: sem autenticação, rate limiting nem sistema antifraude; a sala é apenas para desenvolvimento local.

Não armazene informações privadas no chat público. Os endpoints REST legados de conversas privadas retornam HTTP 501.

A configuração atual utiliza `schema.sql` apenas para desenvolvimento. Versionamento por Flyway e migração de Spring Boot são próximas etapas, **ainda não implementadas**.

<<<<<<< Updated upstream
Veja [CHANGELOG](CHANGELOG.md), [decisões](docs/DECISIONS.md) e [plano](docs/DEVELOPMENT_PLAN.md).
=======
## Troubleshooting

### Problemas Comuns
1. **Porta em uso**: Alterar `server.port` no `application.properties`
2. **CORS**: Verificar `WebConfig.java`
3. **WebSocket**: Confirmar endpoint `/ws`

## Licença

Projeto sob licença MIT.

## Guest session security (stage 1)

Guests still enter with a username; the server now reserves it and issues an opaque token via
`POST /api/chat/session` with `{ "username": "alice", "token": null }`. On browser
reconnection, resend the previously issued token to resume. STOMP `CONNECT` must carry
`Authorization: Bearer <token>` (a STOMP header, never a URL parameter). Private REST
endpoints require the same HTTP Authorization header and verify participation.

Guests are **not permanent user accounts**. This is a single-instance demo session mechanism,
not password-based identity or persistent authentication. Sessions are released on logout,
or expire after disconnect grace/idle time. For production deployment, add persistent
authentication, TLS, rate limiting and server-managed storage. Configure
`CHAT_ALLOWED_ORIGINS` for the frontend origin; default is `http://localhost:4200`.

### Stage 1 smoke test

1. Start the back-end and Angular app; join from two separate browser sessions.
2. Send a private message and join as a third user: the third user must **not**
   receive the private message in `/user/queue/history`.
3. Verify another guest cannot claim a reserved username.
4. Disconnect/reconnect the same guest and verify public chat is functional.
5. Verify `GET /api/chat/private/conversation/alice/bob` without the required
   Authorization header returns unauthorized.
>>>>>>> Stashed changes
