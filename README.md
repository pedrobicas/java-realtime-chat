# Backend - Sistema de Chat Privado

## Visão Geral

Este é o backend do sistema de chat privado desenvolvido em Java com Spring Boot. Ele fornece uma API REST completa e suporte a WebSocket para comunicação em tempo real entre usuários.

## Tecnologias Utilizadas

- **Java 17**
- **Spring Boot 3.3.2**
- **Spring WebSocket (STOMP)**
- **Spring Web MVC**
- **Jackson (JSON)**
- **Maven**

## Estrutura do Projeto

```
src/main/java/com/example/chat/
├── ChatApplication.java                    # Classe principal da aplicação
├── ChatMessage.java                        # Modelo de mensagem
├── User.java                               # Modelo de usuário
├── ChatService.java                        # Serviço principal do chat
├── ChatController.java                     # Controlador REST e WebSocket
├── WebSocketConfig.java                    # Configuração WebSocket
├── WebConfig.java                          # Configuração CORS
├── PrivateConversationInfo.java            # Informações de conversas privadas
└── PrivateChatNotificationService.java     # Serviço de notificações
```

## Funcionalidades Implementadas

### 🚀 Chat Público
- Conexão via WebSocket
- Envio e recebimento de mensagens
- Indicador de digitação
- Lista de usuários online
- Histórico de mensagens

### 🔐 Chat Privado
- Mensagens privadas entre usuários
- Gerenciamento de conversas
- Contadores de mensagens não lidas
- Confirmação de leitura
- Notificações em tempo real

### 📡 WebSocket Endpoints

#### Mensagens Públicas
- `/app/chat.send` - Enviar mensagem pública
- `/app/chat.addUser` - Adicionar usuário ao chat
- `/app/chat.leave` - Usuário sai do chat
- `/app/chat.typing` - Indicador de digitação

#### Mensagens Privadas
- `/app/chat.private` - Enviar mensagem privada
- `/user/queue/private` - Receber mensagens privadas
- `/user/queue/notifications` - Notificações gerais
- `/user/queue/read-receipts` - Confirmações de leitura
- `/user/queue/typing` - Indicador de digitação privada

#### Tópicos Públicos
- `/topic/public` - Mensagens públicas
- `/topic/users` - Lista de usuários
- `/topic/user-status` - Mudanças de status

### 🌐 API REST Endpoints

#### Usuários
```
GET  /api/chat/users                    # Listar usuários online
GET  /api/chat/users/{username}         # Obter usuário específico
GET  /api/chat/users/{username}/status  # Status do usuário
GET  /api/chat/users/search?query={}    # Buscar usuários
```

#### Chat Privado
```
GET  /api/chat/private/conversations/{username}           # Conversas do usuário
GET  /api/chat/private/conversation/{user1}/{user2}       # Histórico entre usuários
POST /api/chat/private/mark-read                          # Marcar como lido
```

#### Estatísticas
```
GET  /api/chat/stats                    # Estatísticas do chat
GET  /api/chat/health                   # Health check
```

## Configuração e Execução

### Pré-requisitos
- Java 17 ou superior
- Maven 3.6+

### Executar o Projeto
```bash
# Navegar para o diretório do backend
cd java-realtime-chat

# Compilar o projeto
mvn clean compile

# Executar
mvn spring-boot:run

# Ou usar o wrapper Maven
./mvnw spring-boot:run
```

### Configurações
O arquivo `application.properties` contém todas as configurações necessárias:

```properties
# Porta do servidor
server.port=8080

# Configurações de WebSocket
spring.websocket.max-text-message-size=128KB
spring.websocket.max-binary-message-size=128KB

# Configurações de CORS
spring.web.cors.allowed-origins=*
spring.web.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
```

## Arquitetura

### Camadas da Aplicação

1. **Controller Layer** (`ChatController.java`)
    - Endpoints REST
    - Handlers WebSocket
    - Validação de entrada

2. **Service Layer** (`ChatService.java`)
    - Lógica de negócio
    - Gerenciamento de estado
    - Comunicação WebSocket

3. **Model Layer** (`ChatMessage.java`, `User.java`)
    - Entidades do domínio
    - Validações
    - Serialização JSON

4. **Configuration Layer** (`WebSocketConfig.java`, `WebConfig.java`)
    - Configuração WebSocket
    - Configuração CORS
    - Beans de configuração

### Fluxo de Mensagens Privadas

1. **Cliente envia mensagem** via `/app/chat.private`
2. **Controller recebe** e valida a mensagem
3. **Service processa** e armazena no histórico
4. **WebSocket envia** para remetente e destinatário
5. **NotificationService** envia notificações adicionais

### Gerenciamento de Estado

- **Usuários ativos**: `ConcurrentHashMap<String, User>`
- **Histórico de mensagens**: `List<ChatMessage>` sincronizada
- **Contadores de não lidas**: `ConcurrentHashMap` aninhado
- **Sessões WebSocket**: Mapeamento automático via Spring

## Segurança

### Validações Implementadas
- Verificação de usuários online
- Validação de conteúdo de mensagens
- Sanitização de entrada
- Controle de acesso por sessão

### CORS Configurado
- Permitir origens múltiplas
- Métodos HTTP permitidos
- Headers personalizados
- Credenciais habilitadas

## Monitoramento e Logs

### Logs Configurados
```properties
logging.level.com.example.chat=INFO
logging.level.org.springframework.web.socket=DEBUG
logging.level.org.springframework.messaging=DEBUG
```

### Métricas Disponíveis
- Total de usuários online
- Total de mensagens
- Tempo de atividade do chat
- Top usuários por mensagens

## Testes

### Executar Testes
```bash
# Testes unitários
mvn test

# Testes de integração
mvn verify

# Cobertura de testes
mvn jacoco:report
```

### Tipos de Teste
- **Unitários**: Serviços e modelos
- **Integração**: Controllers e WebSocket
- **End-to-End**: Fluxo completo de mensagens

## Performance

### Otimizações Implementadas
- **ConcurrentHashMap** para acesso concorrente
- **Collections.synchronizedList** para histórico
- **ThreadPoolTaskScheduler** para WebSocket
- **Cache Caffeine** para dados frequentes

### Configurações de WebSocket
- Heartbeat: 10 segundos
- Timeout de conexão: 25 segundos
- Delay de desconexão: 30 segundos
- Limite de mensagem: 128KB

## Troubleshooting

### Problemas Comuns

1. **Porta já em uso**
   ```bash
   # Verificar processos na porta 8080
   netstat -ano | findstr :8080
   
   # Alterar porta no application.properties
   server.port=8081
   ```

2. **Erro de CORS**
    - Verificar configurações no `WebConfig.java`
    - Confirmar origem do frontend

3. **WebSocket não conecta**
    - Verificar logs de WebSocket
    - Confirmar endpoint `/ws`
    - Verificar configurações de firewall

### Logs Úteis
```java
// Log de conexão WebSocket
logger.info("Nova conexão WebSocket estabelecida. SessionId: {}", sessionId);

// Log de mensagem privada
logger.info("Mensagem privada enviada: {} -> {}", sender, recipient);

// Log de erro
logger.error("Erro ao enviar mensagem privada: {}", e.getMessage());
```

## Próximos Passos

### Funcionalidades Futuras
1. **Persistência**: Banco de dados para histórico
2. **Autenticação**: JWT e roles de usuário
3. **Arquivos**: Upload e compartilhamento
4. **Grupos**: Conversas em grupo
5. **Criptografia**: End-to-end encryption

### Melhorias Técnicas
1. **Cache distribuído**: Redis para escalabilidade
2. **Métricas**: Prometheus e Grafana
3. **Health checks**: Liveness e readiness probes
4. **Configuração**: Config Server e profiles

## Contribuição

### Padrões de Código
- **Java**: Convenções Java 17
- **Spring**: Best practices Spring Boot
- **Logging**: SLF4J com logback
- **Testes**: JUnit 5 e Mockito

### Estrutura de Commits
```
feat: adicionar funcionalidade de chat privado
fix: corrigir bug no envio de mensagens
docs: atualizar documentação da API
style: melhorar formatação do código
refactor: refatorar serviço de mensagens
test: adicionar testes para chat privado
```

## Licença

Este projeto está sob a licença MIT. Veja o arquivo LICENSE para mais detalhes.

---

**Backend robusto e escalável para um sistema de chat privado em tempo real! 🚀**

