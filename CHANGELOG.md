# Changelog

## 2.0.0-dev.2 — quality / session isolation
- Geração de `Principal` exclusivo por conexão, separado do apelido público.
- Verificação do apelido do handshake antes de registrar a sessão.
- Registro atômico de apelidos e roteamento individual de histórico/menções.
- Campos das mensagens públicas passam a ser gerados pelo backend (não aceitam ID ou remetente do cliente).
- Sessão interna não é exposta no JSON do usuário e avatars são derivados do apelido definitivo.
- Notificação de saída publicada também quando a conexão WebSocket cai.
- CI usa `mvn verify`; README e documentação de execução autônomos.
- Continua **sem autenticação**; não expor na internet.

## 2.0.0-dev.1
- Histórico público persistido em JDBC, controle de destinos e chat privado desabilitado.
