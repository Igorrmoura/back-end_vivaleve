package vivaleve.proj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configurações da IA, lidas de "pluma.ai.*" no application.yml.
 * A chave da API vem SEMPRE de variável de ambiente (OLLAMA_API_KEY), nunca do código.
 */
@ConfigurationProperties(prefix = "pluma.ai")
public record AiProperties(
        String baseUrl,
        String apiKey,
        String model,
        Double temperature,
        Integer maxTokens,
        Duration timeout,
        Integer historyLimit,
        Integer maxResponseChars
) {
    public AiProperties {
        baseUrl = isBlank(baseUrl) ? "https://ollama.com" : baseUrl;
        model = isBlank(model) ? "gpt-oss:20b" : model;
        temperature = temperature == null ? 0.7 : temperature;
        maxTokens = maxTokens == null ? 600 : maxTokens;
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        historyLimit = historyLimit == null ? 10 : historyLimit;
        maxResponseChars = maxResponseChars == null ? 2000 : maxResponseChars;
    }

    public boolean hasApiKey() {
        return !isBlank(apiKey);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
