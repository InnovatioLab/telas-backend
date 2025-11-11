# 🧩 Plataforma de Gerenciamento de Anúncios Digitais

📖 **Documentação completa da API:**  
➡️ [https://api.telas-ads.com/api/swagger-ui/index.html](https://api.telas-ads.com/api/swagger-ui/index.html)

---

## 📝 Sobre o projeto

Este projeto é uma **plataforma de gerenciamento de anúncios digitais**, desenvolvida em **Java com Spring Boot**, responsável por controlar todo o ciclo de vida de **anúncios, pagamentos e assinaturas**, além de realizar a **sincronização em tempo real com dispositivos físicos (monitores)**.

O sistema foi projetado com foco em **resiliência, integridade dos dados e escalabilidade**, garantindo que nenhum evento de pagamento fosse perdido, mesmo em cenários de falha.

---

## ⚙️ Tecnologias principais

- **Java 17+**
- **Spring Boot**
- **Spring Data JPA**
- **RabbitMQ**
- **Stripe API**
- **PostgreSQL**
- **ShedLock**
- **Docker / Docker Compose**

---

## 🧠 Desafios técnicos e soluções adotadas

### 1. Processamento resiliente de eventos da Stripe
- Os webhooks da Stripe são recebidos de forma **assíncrona**.
- O evento é publicado em uma fila **RabbitMQ**, garantindo que nenhuma mensagem se perca em caso de falha momentânea.
- A resposta ao webhook é quase instantânea, evitando timeouts.

### 2. Garantia de integridade e idempotência
- Cada evento da Stripe possui um **ID único** armazenado no banco de dados.
- Antes de processar uma nova mensagem, o sistema verifica se o ID já foi tratado, evitando **duplicações**.

### 3. Tolerância a falhas
- Implementada uma **Dead Letter Queue (DLQ)** para armazenar eventos que falharem no processamento.
- Nenhum evento é descartado — cada falha pode ser analisada e reprocessada.

### 4. Preparação para escalabilidade
- Uso do **@Version (Optimistic Locking)** no JPA para prevenir condições de corrida.
- **ShedLock** garante que apenas uma instância execute tarefas agendadas simultaneamente.
- Arquitetura preparada para operar com múltiplas instâncias do monólito.

---

## 🚀 Como executar o projeto

### Pré-requisitos
- **Docker** e **Docker Compose** instalados
- **Java 17+**
- **Maven**

### Passos

1. Clone o repositório:
   ```bash
   git clone git@github.com:InnovatioLab/telas-backend.git
   cd telas-backend
   ```
2. Suba os containers:

```bash
cd docker
make up
```

3. Adicione as variáveis de ambientes necessárias, você pode consultá-las clicando <a href="https://github.com/InnovatioLab/telas-backend/blob/main/src/main/resources/application.properties" target="_blank">aqui</a>

4. Execute a aplicação:
```bash
cd ..
mvn spring-boot:run
```


Acesse:
API: http://localhost:8080
Swagger: http://localhost:8080/api/swagger-ui/index.html
