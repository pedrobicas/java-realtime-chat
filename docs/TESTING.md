# Verificações do backend

Execute `mvn verify` com Java 21 / Maven 3.9+.

Cobertura existente:

- Persistência de mensagens públicas em ordem cronológica (H2/JDBC).
- Recusa de eventos privados no repositório público.
- Rejeição de mensagens excessivamente longas e sessões desconhecidas.
- Geração pelo servidor de remetente e identificador de mensagem.
- Rejeição de apelidos duplicados e separação de principal STOMP por sessão.

**Ainda faltam testes de integração STOMP reais** envolvendo WebSocket/SockJS, duas conexões, desconexão abrupta, histórico e autorização. Eles fazem parte do próximo marco, não desta prévia.
