# 🪶 Pluma · API (Back-end)

API REST do **Pluma**, plataforma de apoio emocional e bem-estar. Ela cuida de cadastro e autenticação, registros de humor, diário, histórico e demais dados do app, persistindo tudo em PostgreSQL.

![Java](https://img.shields.io/badge/Java-Spring%20Boot-green)
![Banco](https://img.shields.io/badge/PostgreSQL-336791)
![Build](https://img.shields.io/badge/build-Maven-C71A36)
![Docker](https://img.shields.io/badge/Docker-2496ED)
![Deploy](https://img.shields.io/badge/deploy-Render-46E3B7)

- **Front-end:** [projetovivaleve-5org/Pluma](https://github.com/projetovivaleve-5org/Pluma) (https://projetopluma.vercel.app)

>  O Pluma lida com **dados pessoais sensíveis** (humor e diário). Leia a seção [Segurança e privacidade](#-segurança-e-privacidade) antes de colocar o sistema em uso real.

---

## Sumário

- [Stack](#-stack)
- [Arquitetura](#-arquitetura)
- [Como executar](#-como-executar)
- [Variáveis de ambiente](#-variáveis-de-ambiente)
- [Docker](#-docker)
- [Deploy na Render](#-deploy-na-render)
- [Convenções da API](#-convenções-da-api)
- [Autenticação](#-autenticação)
- [Endpoints](#-endpoints)
- [Modelo de dados](#-modelo-de-dados)
- [Segurança e privacidade](#-segurança-e-privacidade)
- [Problemas comuns](#-problemas-comuns)
- [Testes](#-testes)
- [Contribuição](#-contribuição)

---

## Stack

| Tecnologia | Uso |
|---|---|
| **Java** 21 | Linguagem |
| **Spring Boot** | Framework da aplicação e da API REST |
| **Spring Data JPA** | Persistência e acesso ao banco |
| **PostgreSQL** | Banco de dados (hospedado na Render) |
| **Maven** (`mvnw`) | Build e dependências |
| **Docker** | Empacotamento para deploy |
| **Render** | Hospedagem da API e do banco |
---

## Arquitetura

```text
Cliente (front-end / Postman)
        │  HTTP + JSON
        ▼
  Controller   → recebe a requisição, valida a entrada, devolve a resposta
        ▼
  Service      → regras de negócio
        ▼
  Repository   → acesso ao banco (Spring Data JPA)
        ▼
  PostgreSQL
```

### Estrutura do repositório

```text
back-end_vivaleve/
├── .mvn/wrapper/     # Maven Wrapper
├── docs/             # Documentação (lista de endpoints)
├── src/
│   └── main/java/... # Código-fonte
├── application.yml   # Configuração da aplicação
├── pom.xml           # Dependências e build
├── mvnw, mvnw.cmd    # Maven Wrapper
└── README.md
```

<!-- VERIFICAR: detalhar os pacotes reais dentro de src/main/java (controller, service, repository, model/entity, dto, security, config, exception) -->

---

##  Como executar

### Pré-requisitos

- Java 21 (JDK)
- PostgreSQL acessível (local, Docker ou Render)
- Git

### Passo a passo

```bash
git clone https://github.com/projetovivaleve-5org/back-end_vivaleve.git
cd back-end_vivaleve
```

1. Configure as [variáveis de ambiente](#-variáveis-de-ambiente).
2. Rode a aplicação:

```bash
# Linux / macOS
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

3. A API sobe em `http://localhost:8080` <!-- VERIFICAR porta -->.

### Gerar o .jar

```bash
./mvnw clean package
java -jar target/*.jar
```

---

## Variáveis de ambiente

**Nunca** versione senhas, tokens ou segredos. Use variáveis de ambiente (ou o painel da Render) e mantenha o `.env` no `.gitignore`.

| Variável | Descrição | Exemplo |
|---|---|---|
| `DB_URL` | URL JDBC do PostgreSQL | `jdbc:postgresql://host:5432/pluma` |
| `DB_USER` | Usuário do banco | `pluma_user` |
| `DB_PASSWORD` | Senha do banco | *(segredo)* |
| `JWT_SECRET` | Segredo para assinar tokens | *(segredo)* |
| `PORT` | Porta da aplicação | `8080` |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas (front-end) | `https://projetopluma.vercel.app` |

<!-- VERIFICAR: trocar pelos nomes reais usados no application.yml -->

---

## Docker

```bash
docker build -t pluma-api .
docker run -p 8080:8080 --env-file .env pluma-api
```

<!-- VERIFICAR: confirmar o Dockerfile e ajustar os comandos -->

---

## Deploy na Render

- A API roda como **Web Service com Docker**.
- O banco é uma instância **PostgreSQL da Render**.
- Configure as variáveis de ambiente no painel do serviço.
- Em planos gratuitos, a Render pode "hibernar" o serviço após um tempo sem uso, e a **primeira requisição pode demorar** para responder.

---

##  Convenções da API

| Item | Padrão |
|---|---|
| **Base URL (produção)** | `https://<URL-DA-API-NA-RENDER>` <!-- VERIFICAR --> |
| **Base URL (local)** | `http://localhost:8080` |
| **Formato** | JSON (`Content-Type: application/json`) |
| **Autenticação** | Token no header `Authorization: Bearer <token>` <!-- VERIFICAR --> |
| **Datas** | ISO 8601 (`2026-11-16T10:30:00`) |

### Códigos de status

| Código | Significado |
|---|---|
| `200 OK` | Requisição bem-sucedida |
| `201 Created` | Recurso criado |
| `204 No Content` | Sucesso sem corpo de resposta (ex.: exclusão) |
| `400 Bad Request` | Dados inválidos |
| `401 Unauthorized` | Token ausente, inválido ou expirado |
| `403 Forbidden` | Sem permissão para o recurso |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Conflito (ex.: e-mail já cadastrado) |
| `500 Internal Server Error` | Erro inesperado no servidor |

### Formato de erro (exemplo ilustrativo)

```json
{
  "status": 400,
  "mensagem": "Dados inválidos",
  "detalhes": ["email: formato inválido"]
}
```

<!-- VERIFICAR: ajustar ao formato de erro realmente retornado pela API -->

---

## Autenticação

Fluxo geral:

1. A pessoa se **cadastra** e faz **login** pelos endpoints de autenticação.
2. A API devolve um **token**.
3. O front-end envia esse token no header `Authorization` em todas as rotas protegidas.
4. Cada pessoa só acessa **os próprios dados**.

```http
GET /rota-protegida
Authorization: Bearer <token>
```

<!-- VERIFICAR: confirmar o mecanismo (JWT, sessão, etc.), o tempo de expiração do token e se existe refresh token -->

---

## Endpoints

A lista completa e oficial está no documento **"Pluma: Endpoints da API"** (pasta `docs/`). Abaixo, a referência organizada por recurso. O objetivo é que a soma de todas as tabelas dê **34 endpoints**.

> Legenda de **Auth**:  pública · exige token

### Exemplo de como documentar cada endpoint (ilustrativo, substitua pelos reais)

**`POST /auth/login`**: autentica uma pessoa.

Requisição:

```json
{
  "email": "pessoa@exemplo.com",
  "senha": "********"
}
```

Resposta `200 OK`:

```json
{
  "token": "<jwt>",
  "usuario": { "id": 1, "nome": "Pessoa" }
}
```

Erros possíveis: `400` (dados inválidos), `401` (credenciais incorretas).

---

### 1. Autenticação e usuário

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `…` | 🔓 | Cadastro de usuário |
| `POST` | `…` | 🔓 | Login |
| `GET` | `…` | 🔒 | Dados do usuário logado |
| `PUT` | `…` | 🔒 | Atualizar dados do usuário |
| `DELETE` | `…` | 🔒 | Excluir conta e dados |

### 2. Humor

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `…` | 🔒 | Registrar humor |
| `GET` | `…` | 🔒 | Listar registros de humor |
| `GET` | `…` | 🔒 | Buscar registro por id |
| `PUT` | `…` | 🔒 | Atualizar registro |
| `DELETE` | `…` | 🔒 | Remover registro |

### 3. Diário

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `…` | 🔒 | Criar entrada |
| `GET` | `…` | 🔒 | Listar entradas |
| `GET` | `…` | 🔒 | Buscar entrada por id |
| `PUT` | `…` | 🔒 | Editar entrada |
| `DELETE` | `…` | 🔒 | Remover entrada |

### 4. Histórico

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `…` | 🔒 | Consultar histórico (por período) |

### 5. Respiração, chat e configurações

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `…` | `…` | 🔒 | … |

<!-- VERIFICAR: esta lista de recursos é apenas um esqueleto baseado nas telas do app. Substitua pelas rotas reais do documento de endpoints e confira se a soma é 34. Para cada endpoint, informe também o corpo da requisição, o formato da resposta e os erros possíveis. -->

---

## Modelo de dados

Entidades principais <!-- VERIFICAR: confirmar com o diagrama do grupo -->:

| Entidade | Descrição | Campos principais |
|---|---|---|
| **Usuário** | Pessoa cadastrada | id, nome, e-mail, senha (hash), criado em |
| **Registro de humor** | Humor registrado em um dia | id, usuário, humor, data |
| **Entrada de diário** | Reflexão escrita | id, usuário, texto, data |

```text
Usuário 1 ──── N Registro de humor
Usuário 1 ──── N Entrada de diário
```

---

##  Segurança e privacidade

- Senhas armazenadas **somente com hash** (ex.: BCrypt), nunca em texto puro.
- Comunicação sempre por **HTTPS** em produção.
- Segredos (banco, JWT) **somente em variáveis de ambiente**.
- **CORS** restrito à origem do front-end.
- Cada requisição protegida deve validar que o recurso pertence à pessoa autenticada.
- Os dados de humor e diário são **sensíveis** (LGPD): coletar o mínimo necessário, ter consentimento claro e permitir **exclusão de dados**.
- Não registrar conteúdo do diário em logs.

---

## Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| Primeira requisição muito lenta em produção | Serviço hibernado na Render (plano gratuito) | Aguardar ou usar plano sem hibernação |
| Erro de CORS no navegador | Origem do front não liberada | Incluir a URL do front em `CORS_ALLOWED_ORIGINS` |
| `401 Unauthorized` | Token ausente ou expirado | Fazer login novamente e enviar o header `Authorization` |
| Falha ao conectar no banco | Variáveis `DB_*` incorretas | Conferir URL, usuário e senha |

---

## Testes

```bash
./mvnw test
```

<!-- VERIFICAR: informar se já existem testes e a cobertura; sugestão de roadmap: testes de integração dos endpoints e coleção do Postman/Insomnia em docs/ -->

---

##  Contribuição

1. Faça um *fork* e crie uma branch: `git checkout -b feat/minha-melhoria`.
2. Commits pequenos e descritivos.
3. Abra um *Pull Request* descrevendo o que mudou.

## Equipe

Tsukii3 |

Lucas |

Igor Moura | 

Gustavo Ferrão |


---

<p align="center">Projeto acadêmico · Pluma 🪶</p>
