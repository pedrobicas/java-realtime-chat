# Verificações do backend

Execute `mvn verify` com Java 21 / Maven 3.9+.

Cobertura existente:

- Persistência de mensagens públicas em ordem cronológica (H2/JDBC).
- Recusa de eventos privados no repositório público.
- Rejeição de mensagens excessivamente longas e sessões desconhecidas.
- Geração pelo servidor de remetente e identificador de mensagem.
- Rejeição de apelidos duplicados de outra identidade e roteamento por principal opaco.
- Token obrigatório, retomada da identidade e renovação do principal após logout.
- Reconexão substitui a sessão anterior e ignora sua desconexão atrasada; JOIN repetido é idempotente.
- Inicialização completa do servidor, rotas sem duplicidade e chat privado desabilitado.
- Clientes STOMP reais por WebSocket nativo e SockJS: reserva via REST, JOIN autenticado, mensagem pública, histórico persistido e reconexão.

Ainda faltam validação com PostgreSQL/Nginx no Docker Compose, testes de queda abrupta de rede e testes de carga. Os testes automatizados usam H2 em memória e um servidor local em porta aleatória.
