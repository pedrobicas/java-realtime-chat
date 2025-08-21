# 💬 Chat em Tempo Real em Java

Uma aplicação minimalista de **chat em tempo real** construída com
**Spring Boot 3**, **STOMP sobre WebSocket** e um frontend simples em
HTML/Thymeleaf.\
Os usuários podem entrar com um apelido e trocar mensagens
instantaneamente em uma sala pública.

------------------------------------------------------------------------

## 🚀 Funcionalidades

-   🔌 Comunicação em tempo real com WebSocket + STOMP (fallback
    SockJS)\
-   👥 Sala pública com notificações de entrada e saída\
-   🎨 Interface responsiva com tema escuro (HTML + CSS puro)\
-   📦 Fácil de rodar com Maven (nenhuma configuração extra necessária)

------------------------------------------------------------------------

## 📂 Estrutura do Projeto

    src/main/java/com/example/chat
    ├── ChatApplication.java     # Classe principal
    ├── WebSocketConfig.java     # Configuração do WebSocket/STOMP
    ├── ChatMessage.java         # DTO para mensagens
    └── ChatController.java      # Controlador para envio/recebimento
    src/main/resources
    ├── application.properties   # Configurações da aplicação
    └── templates/index.html     # Interface do chat

------------------------------------------------------------------------

## 🛠️ Como Executar

### Pré-requisitos

-   Java 17+\
-   Maven 3.8+

### Rodando localmente

``` bash
# clone o repositório
git clone https://github.com/pedrobicas/realtime-chat.git
cd realtime-chat

# inicie a aplicação
mvn spring-boot:run
```

Abra no navegador: **http://localhost:8080** 🚀

------------------------------------------------------------------------

## 🔌 Endpoints WebSocket

-   Endpoint STOMP: `/ws`\
-   Assinaturas:
    -   `/topic/public` → mensagens públicas\
-   Destinos da aplicação:
    -   `/app/chat.send` → enviar mensagem\
    -   `/app/chat.addUser` → registrar novo usuário


------------------------------------------------------------------------


## 📝 Licença

Este projeto está licenciado sob a **MIT License** -- fique à vontade
para usar e melhorar.
