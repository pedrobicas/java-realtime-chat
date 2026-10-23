# Decisões da v2 (desenvolvimento)

1. **Evolução incremental:** preservar a interface e o modelo de mensagens do chat original.
2. **Armazenamento público:** JDBC salva mensagens públicas antes do broadcast; banco H2 local e PostgreSQL no Compose.
3. **Identidade de sessão:** o nickname é uma escolha temporária do visitante, nunca uma credencial. Cada WebSocket possui um `Principal` de roteamento aleatório e único. O JOIN valida o nickname obtido do handshake. Mensagens privadas permanecem desligadas.
4. **Protocolo:** `/app/chat.send` aceita somente conteúdo de uma mensagem pública; o servidor cria metadados. Clientes não podem publicar diretamente em `/topic` ou `/queue`.
5. **Qualidade:** CI deve rodar testes; relatórios locais e migrações futuras são registrados sem declarar produção pronta antes de validação.
6. **Roadmap:** considerar Flyway, clientes STOMP via npm, migração gradual de frameworks, testes de integração com clientes reais, autenticação e rate limiting antes de publicação.
