package vivaleve.proj.service.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import vivaleve.proj.config.AiProperties;
import vivaleve.proj.service.ai.AiClient.AiChatMessage;
import vivaleve.proj.service.ai.AiClient.AiCompletion;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Testa a integração HTTP com o Ollama Cloud sem rede, simulando o servidor. */
class OllamaCloudClientTest {

    private static final List<AiChatMessage> PROMPT =
            List.of(AiChatMessage.system("regras"), AiChatMessage.user("oi"));

    private MockRestServiceServer server;
    private OllamaCloudClient client;

    @BeforeEach
    void setUp() {
        client = clientWithKey("test-key");
    }

    private OllamaCloudClient clientWithKey(String key) {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://ollama.test");
        server = MockRestServiceServer.bindTo(builder).build();
        AiProperties props = new AiProperties("https://ollama.test", key, "gpt-oss:20b", 0.7, 600, null, null, null);
        return new OllamaCloudClient(builder.build(), props);
    }

    @Test
    void enviaRequisicaoNoFormatoDoOllamaELeResposta() {
        server.expect(requestTo("https://ollama.test/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-oss:20b"))
                .andExpect(jsonPath("$.stream").value(false))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("oi"))
                .andExpect(jsonPath("$.options.temperature").value(0.7))
                .andExpect(jsonPath("$.options.num_predict").value(600))
                .andRespond(withSuccess("""
                        {
                          "model": "gpt-oss:20b",
                          "created_at": "2026-11-10T12:00:00Z",
                          "message": {"role": "assistant", "content": "Olá! Como você está?", "thinking": "..."},
                          "done": true,
                          "done_reason": "stop",
                          "prompt_eval_count": 42,
                          "eval_count": 7
                        }
                        """, MediaType.APPLICATION_JSON));

        AiCompletion completion = client.complete(PROMPT);

        server.verify();
        assertThat(completion.content()).isEqualTo("Olá! Como você está?");
        assertThat(completion.model()).isEqualTo("gpt-oss:20b");
        assertThat(completion.promptTokens()).isEqualTo(42);
        assertThat(completion.responseTokens()).isEqualTo(7);
        assertThat(completion.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void erroHttpViraAiClientException() {
        server.expect(requestTo("https://ollama.test/api/chat")).andRespond(withServerError());

        assertThatThrownBy(() -> client.complete(PROMPT)).isInstanceOf(AiClientException.class);
    }

    @Test
    void respostaSemConteudoViraAiClientException() {
        server.expect(requestTo("https://ollama.test/api/chat"))
                .andRespond(withSuccess("""
                        {"model":"gpt-oss:20b","message":{"role":"assistant","content":""},"done":true}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.complete(PROMPT))
                .isInstanceOf(AiClientException.class)
                .hasMessageContaining("vazia");
    }

    @Test
    void semChaveNaoFazChamadaHttp() {
        OllamaCloudClient semChave = clientWithKey("");

        assertThatThrownBy(() -> semChave.complete(PROMPT))
                .isInstanceOf(AiClientException.class)
                .hasMessageContaining("OLLAMA_API_KEY");
        server.verify(); // nenhuma requisição esperada, nenhuma feita
    }
}
