# Changelog

## Correções de merge — 2026-10-26
- Resolvidos conflitos gravados em controllers, serviço, configuração, propriedades e README.
- Removidos métodos e rota de saúde duplicados; testes atualizados para a API atual.
- Integrados token de visitante, principal opaco, JOIN autenticado e reconexão com proteção contra desconexão antiga.
- Preservados histórico JDBC, mensagens canônicas e bloqueio de conversas privadas.
- Adicionados testes com servidor real via WebSocket/SockJS e detecção de conflitos no CI.

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
