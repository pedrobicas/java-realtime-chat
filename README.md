# Realtime Chat v2 — Spring Boot API

Backend Java / Spring Boot para o [frontend Angular](https://github.com/pedrobicas/web-realtime-chat). Branch de desenvolvimento: `develop/v2`.

**Estado:** `2.0.0-dev.2` (prévia local; não publicar diretamente na Internet). O chat público usa STOMP sobre WebSocket, persiste mensagens no banco e restabelece o histórico em novas sessões. Conversas privadas continuam desligadas até autenticação e autorização completas.

## Execução local

Requisitos: **JDK 21** e **Maven 3.9+**.

```sh
mvn spring-boot:run
```

Por padrão: `http://localhost:8081`; banco H2 persistido em `./realtimechat-dev.mv.db` (arquivo ignorado pelo Git).

Para executar o **sistema completo** (PostgreSQL, backend e Angular via Nginx) clone ambos os repositórios como pastas irmãs e execute `docker compose up --build` na pasta `web-realtime-chat`. Veja as instruções em [`web-realtime-chat/README.md`](https://github.com/pedrobicas/web-realtime-chat/blob/develop/v2/README.md).

## Verificação

```sh
mvn verify
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

Veja [CHANGELOG](CHANGELOG.md), [decisões](docs/DECISIONS.md) e [plano](docs/DEVELOPMENT_PLAN.md).
