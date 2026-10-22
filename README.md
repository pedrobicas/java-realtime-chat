# Realtime Chat v2 — Backend (development)

Spring Boot + WebSocket/STOMP backend for the [Realtime Chat Angular frontend](https://github.com/pedrobicas/web-realtime-chat).

**Version:** `2.0.0-dev.1` — development preview; not production-ready.

## Run locally

Requirements: JDK 21 and Maven. By default the service listens on `http://localhost:8081` and persists public-message history in a local H2 database file.

```sh
mvn spring-boot:run
```

The frontend, running on `http://localhost:4200`, proxies `/api` and `/ws` to this service.

Try `GET http://localhost:8081/api/chat/health` and `GET http://localhost:8081/api/chat/version`.

## Checks

```sh
mvn test
mvn package
```

## v2 changes

- Public-message persistence via JDBC with H2 locally or PostgreSQL in the combined Docker workspace.
- Public message history, session-tied sender identity and validation.
- Origin restrictions controlled by `CHAT_ALLOWED_ORIGINS`.
- Legacy private-chat endpoints intentionally return HTTP 501 while authentication is not implemented.

## Deployment and security

**Do not expose this development preview publicly.** It has no real accounts or authentication, protection against spam or rate limiting. Name-only identities are not proof of identity. The development JDBC schema uses `schema.sql` rather than versioned migrations. The backend needs to run as an application process; GitHub Pages cannot run Java or a database.

For a local demonstration using PostgreSQL and Nginx, download the **combined workspace ZIP** from the ChatGPT conversation. The root `compose.yaml` must be run with sibling `backend/` and `frontend/` folders.

## Project state

This repository retains its original history. The v2 preview should be developed on a dedicated `develop/v2` branch and validated before being merged into `main`.
