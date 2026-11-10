package vivaleve.proj.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import vivaleve.proj.config.AiProperties;

import java.util.List;
import java.util.Map;

/**
 * Integração com a API do Ollama Cloud (PL-23).
 * POST {baseUrl}/api/chat com "Authorization: Bearer OLLAMA_API_KEY".
 * Docs: https://docs.ollama.com/cloud
 */
@Slf4j
@Component
public class OllamaCloudClient implements AiClient {

    private final RestClient restClient;
    private final AiProperties props;

    public OllamaCloudClient(@Qualifier("ollamaRestClient") RestClient restClient, AiProperties props) {
        this.restClient = restClient;
        this.props = props;
    }

    @Override
    public AiCompletion complete(List<AiChatMessage> messages) {
        if (!props.hasApiKey()) {
            throw new AiClientException("OLLAMA_API_KEY não configurada");
        }

        OllamaChatRequest body = new OllamaChatRequest(
                props.model(),
                messages,
                false,
                Map.of("temperature", props.temperature(), "num_predict", props.maxTokens())
        );

        long start = System.nanoTime();
        OllamaChatResponse response;
        try {
            response = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.apiKey())
                    .body(body)
                    .retrieve()
                    .body(OllamaChatResponse.class);
        } catch (RestClientException e) {
            // Não loga o corpo da requisição: pode conter desabafos do usuário.
            log.warn("Falha ao chamar Ollama Cloud (modelo {}): {}", props.model(), e.getMessage());
            throw new AiClientException("Falha ao chamar o Ollama Cloud", e);
        }
        long latencyMs = (System.nanoTime() - start) / 1_000_000;

        if (response == null || response.message() == null
                || response.message().content() == null || response.message().content().isBlank()) {
            throw new AiClientException("Ollama Cloud devolveu resposta vazia");
        }

        return new AiCompletion(
                response.message().content(),
                response.model() != null ? response.model() : props.model(),
                response.promptEvalCount(),
                response.evalCount(),
                latencyMs
        );
    }

    /** Corpo enviado para /api/chat. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OllamaChatRequest(String model, List<AiChatMessage> messages, boolean stream, Map<String, Object> options) {
    }

    /** Campos da resposta de /api/chat que usamos (o resto é ignorado). */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaChatResponse(
            String model,
            OllamaMessage message,
            Boolean done,
            @JsonProperty("prompt_eval_count") Integer promptEvalCount,
            @JsonProperty("eval_count") Integer evalCount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OllamaMessage(String role, String content) {
    }
}
