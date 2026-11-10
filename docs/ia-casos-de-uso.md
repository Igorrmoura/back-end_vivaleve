# IA no Pluma

Camada de IA do back-end, usando o **Ollama Cloud** (`https://ollama.com/api/chat`) como modelo do chat.

| Card | O que foi feito | Onde |
|---|---|---|
| PL-22 | Casos de uso definidos (abaixo) | `model/enums/AiUseCase.java`, este arquivo |
| PL-23 | Serviço de integração com a IA | `service/ai/AiClient.java`, `OllamaCloudClient.java`, `AiService.java`, `controller/AiController.java`, `config/AiConfig.java` |
| PL-24 | Prompts e respostas padronizadas | `src/main/resources/prompts/*.txt`, `service/ai/PromptFactory.java`, `dto/AiDtos.java` (`AiResponse`) |
| PL-25 | Armazenamento das interações | `model/AiConversationEntity.java`, `model/AiMessageEntity.java`, `repository/Ai*Repository.java` |
| PL-26 | Testes da camada de IA | `src/test/java/vivaleve/proj/**/Ai*Test.java`, `CrisisDetectorTest`, `PromptFactoryTest`, `OllamaCloudClientTest` |

---

## PL-22 — Casos de uso

### 1. `CHAT_APOIO` — Chat de apoio emocional (tela `chat.html`)
- **Quem usa:** qualquer usuário logado, na aba de chat.
- **Entrada:** mensagem livre (até 2000 caracteres) + `conversationId` opcional para continuar a conversa.
- **O que a IA faz:** escuta, valida o sentimento e sugere no máximo uma ação prática por vez (respiração, pausa, diário).
- **Contexto enviado:** prompt de sistema + últimas 10 mensagens da conversa (`pluma.ai.history-limit`).
- **Substitui:** as respostas fixas por palavra-chave que hoje estão em `js/chat.js` (`analisarTexto` / `getRandomResponse`).

### 2. `INSIGHT_HUMOR` — Leitura das estatísticas de humor (tela `historico.html`)
- **Entrada:** `userId` e `days` (7 ou 30).
- **O que a IA faz:** transforma o `StatsSummary` (já calculado pelo `StatsService`) em um texto curto e gentil: um ponto positivo real + um cuidado, sem inventar números.

### 3. `REFLEXAO_REGISTRO` — Comentário sobre um registro (telas `humor.html` / `diario.html`)
- **Entrada:** id do registro (`EntryEntity`) do próprio usuário.
- **O que a IA faz:** comenta humor + atividades + nota em até 2 parágrafos, sem conclusões clínicas.

### Regra transversal — Detecção de risco (sem IA)
- `CrisisDetector` roda **antes** do modelo em toda mensagem do chat e na nota do registro.
- Se detectar expressões de risco (ex.: "não quero mais viver", "me machucar"), **não chama o modelo**: devolve na hora a resposta fixa de `prompts/resposta-crise.txt` com CVV 188 e SAMU 192, `status = CRISIS` e `helpResources` preenchido.
- Motivo: o modelo pode falhar, demorar ou responder mal justamente no momento mais crítico.
- É uma rede de segurança por palavras-chave, não um classificador clínico.

### Fora do escopo (de propósito)
- Diagnóstico, indicação de remédios, terapia. O prompt de sistema proíbe isso explicitamente.
- Assuntos fora de bem-estar (a IA redireciona com gentileza).

---

## PL-23 — Integração

```
Front (chat.js) ──POST /v1/ai/chat──▶ AiController ──▶ AiService
                                                       ├─ CrisisDetector  (risco? resposta fixa)
                                                       ├─ PromptFactory   (monta system + histórico)
                                                       ├─ AiClient ──┬─▶ GroqClient        ──▶ api.groq.com/openai/v1/chat/completions
                                                       │             └─▶ OllamaCloudClient ──▶ ollama.com/api/chat
                                                       └─ AiConversationRepository / AiMessageRepository
```

- `AiClient` é uma interface: trocar de provedor = nova implementação, sem mexer no resto.
- **Provedor escolhido por `AI_PROVIDER`**: `groq` (plano gratuito, com limite de requisições) ou `ollama` (Ollama Cloud é pago por token; também aceita Ollama local em `http://localhost:11434`).
- Se o provedor falhar (sem chave, timeout, erro HTTP, limite 429 do Groq, falta de crédito 402 do Ollama, resposta vazia), o usuário **não recebe erro**: recebe `status = FALLBACK` com o texto de `prompts/resposta-fallback.txt`.
- Nos modelos `openai/gpt-oss-*` do Groq, enviamos `reasoning_effort: low` para o raciocínio interno não consumir todo o limite de tokens.
- Chamadas ao modelo ficam fora de transação do banco (podem levar segundos).

### Configuração (arquivo `.env` local ou variáveis de ambiente no Render)

Copie `.env.example` para `.env` na raiz do back. O `.env` está no `.gitignore`.

| Variável | Padrão | Observação |
|---|---|---|
| `AI_PROVIDER` | `ollama` | `groq` ou `ollama`. O `.env.example` já vem com `groq`. |
| `GROQ_API_KEY` | (vazio) | Chave grátis em https://console.groq.com/keys. Nunca commitar. |
| `GROQ_MODEL` | `openai/gpt-oss-20b` | Outros: `openai/gpt-oss-120b`, `qwen/qwen3.8-27b` (ver https://console.groq.com/docs/models). |
| `OLLAMA_API_KEY` | (vazio) | Só com `AI_PROVIDER=ollama`. https://ollama.com/settings/keys (exige créditos). |
| `OLLAMA_MODEL` | `gpt-oss:20b` | Na API cloud, sem o sufixo `-cloud`. |
| `OLLAMA_BASE_URL` | `https://ollama.com` | `http://localhost:11434` para usar o Ollama instalado no PC (grátis). |
| `SERVER_PORT` | `8080` | Troque se a porta estiver ocupada. |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5500,http://127.0.0.1:5500` | Adicionar a URL do front na Vercel. |

No Render: *Environment* → adicionar `AI_PROVIDER`, `GROQ_API_KEY` e `CORS_ALLOWED_ORIGINS` (com a URL da Vercel).

---

## PL-24 — Prompts e respostas padronizadas

**Prompts** ficam em texto puro em `src/main/resources/prompts/` para qualquer pessoa do grupo revisar sem mexer em Java:

- `sistema.txt` — persona da Pluma, limites (não diagnosticar, CVV), formato (pt-BR, até N palavras, sem markdown).
- `chat-apoio.txt`, `insight-humor.txt`, `reflexao-registro.txt` — instruções de cada caso de uso.
- `resposta-crise.txt`, `resposta-fallback.txt` — respostas fixas.

**Pós-processamento** (`PromptFactory.sanitizeReply`): remove blocos `<think>`, títulos/negrito de markdown, linhas em branco extras e corta em `max-response-chars` (2000).

**Envelope padrão** — toda rota de IA devolve `AiResponse`:

```json
{
  "conversationId": 12,
  "messageId": 34,
  "useCase": "CHAT_APOIO",
  "status": "SUCCESS",
  "reply": "Sinto muito que o dia esteja pesado...",
  "crisisDetected": false,
  "helpResources": [],
  "model": "gpt-oss:20b",
  "createdAt": "2026-11-10T14:03:21"
}
```

| `status` | Significado | O que o front faz |
|---|---|---|
| `SUCCESS` | Resposta do modelo | Mostra `reply` |
| `FALLBACK` | Modelo indisponível | Mostra `reply` normalmente |
| `CRISIS` | Risco detectado | Mostra `reply` e `helpResources` em destaque (ex.: card vermelho com botão "Ligar 188") |

---

## PL-25 — Armazenamento

Tabelas criadas pelo Hibernate (`ddl-auto: update`):

**`ia_conversas`**: `id`, `usuario_id`, `caso_uso`, `titulo`, `ultima_mensagem_em`, `created_at`, `updated_at`

**`ia_mensagens`**: `id`, `conversa_id`, `papel` (USER/ASSISTANT), `conteudo` (TEXT), `status` (RECEIVED/SUCCESS/FALLBACK/CRISIS), `modelo`, `tokens_prompt`, `tokens_resposta`, `latencia_ms`, `risco_detectado`, `created_at`, `updated_at`

Isso permite: retomar conversas, mostrar histórico, medir custo (tokens) e tempo de resposta, e auditar quantas vezes o fallback/crise foi acionado.

> Privacidade: as mensagens são dados sensíveis de saúde (LGPD, art. 11). O log da aplicação nunca imprime o conteúdo das mensagens. O usuário pode apagar a conversa (`DELETE`).

---

## Rotas

| Método | Rota | Corpo / params |
|---|---|---|
| POST | `/v1/ai/chat` | `{ "userId": 1, "conversationId": null, "message": "Estou ansioso", "userName": "Ana" }` |
| POST | `/v1/ai/insights/mood` | `{ "userId": 1, "days": 7, "userName": "Ana" }` |
| POST | `/v1/ai/entries/{entryId}/reflection` | `{ "userId": 1, "userName": "Ana" }` |
| GET | `/v1/ai/conversations?userId=1` | lista de conversas |
| GET | `/v1/ai/conversations/{id}/messages?userId=1` | mensagens em ordem |
| DELETE | `/v1/ai/conversations/{id}?userId=1` | 204 |

Erros: `400` (validação, `days` diferente de 7/30), `404` (usuário, conversa ou registro não encontrado / de outro usuário).

> Assim como as outras rotas do projeto, o `userId` ainda vem na requisição. Quando o JWT do `docs/estrutura.md` entrar, ele deve sair do corpo e vir do token.

### Ligando no front (`js/chat.js`)

Hoje o `chat.js` usa IDs (`chat-input`, `chat-mensagens`, `btn-enviar`) que **não batem** com o `chat.html` (`chatInput`, `chatMessages`, `sendBtn`), então o chat não funciona nem com as respostas fixas. Ao integrar, trocar `simularResposta` por algo como:

```js
const API_URL = 'https://SEU-BACKEND.onrender.com';

async function perguntarPluma(texto) {
  const user = JSON.parse(localStorage.getItem('pluma_user') || '{}');
  const conversationId = localStorage.getItem('pluma_chat_conversation_id');

  const res = await fetch(`${API_URL}/v1/ai/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      userId: user.id,
      conversationId: conversationId ? Number(conversationId) : null,
      message: texto,
      userName: user.name
    })
  });
  if (!res.ok) throw new Error('Falha no chat: ' + res.status);

  const data = await res.json();
  localStorage.setItem('pluma_chat_conversation_id', data.conversationId);
  return data; // usar data.reply; se data.status === 'CRISIS', destacar data.helpResources
}
```

Obs.: o login do front ainda é só localStorage; o `user.id` precisa vir do back (`POST /v1/users`) para isso funcionar.

---

## PL-26 — Testes

Rodar: `./mvnw test` (não precisa de Postgres nem de chave do Ollama — usa H2 em memória e o Ollama simulado).

| Classe | Tipo | O que cobre |
|---|---|---|
| `CrisisDetectorTest` | unitário | frases de risco com/sem acento, falsos positivos comuns, vazio |
| `PromptFactoryTest` | unitário | templates carregam, placeholders preenchidos, ordem system/histórico/usuário, descrição de stats e registro, sanitização e corte |
| `OllamaCloudClientTest` | HTTP simulado (`MockRestServiceServer`) | formato do JSON enviado, header Bearer, leitura de tokens, erro 500, resposta vazia, sem chave |
| `AiServiceTest` | unitário (Mockito) | sucesso, crise não chama modelo, fallback, histórico sem fallbacks, 404 de usuário/conversa/registro de outro usuário, insight e reflexão |
| `AiControllerTest` | `@WebMvcTest` | rotas, validação (400), 404, 204, formato do envelope |
| `AiPersistenceIntegrationTest` | integração (H2) | conversa de 2 turnos gravada com tokens/latência/modelo, listagem e exclusão |
