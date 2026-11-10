package vivaleve.proj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configurações da IA, lidas de "pluma.ai.*" no application.yml.
 * As chaves de API vêm SEMPRE de variável de ambiente / .env, nunca do código.
 *
 * provider escolhe quem responde: "ollama" (padrão) ou "groq".
 * baseUrl / apiKey / model são do Ollama; os do Groq ficam em {@link Groq}.
 * temperature, maxTokens, timeout etc. valem para os dois.
 */
@ConfigurationProperties(prefix = "pluma.ai")
public record AiProperties(
        String provider,
        String baseUrl,
        String apiKey,
        String model,
        Double temperature,
        Integer maxTokens,
        Duration timeout,
        Integer historyLimit,
        Integer maxResponseChars,
        Groq groq
) {
    public AiProperties {
        provider = isBlank(provider) ? "ollama" : provider.trim().toLowerCase();
        baseUrl = isBlank(baseUrl) ? "https://ollama.com" : baseUrl;
        model = isBlank(model) ? "gpt-oss:20b" : model;
        temperature = temperature == null ? 0.7 : temperature;
        maxTokens = maxTokens == null ? 600 : maxTokens;
        timeout = timeout == null ? Duration.ofSeconds(30) : timeout;
        historyLimit = historyLimit == null ? 10 : historyLimit;
        maxResponseChars = maxResponseChars == null ? 2000 : maxResponseChars;
        groq = groq == null ? new Groq(null, null, null, null) : groq;
    }

    public boolean hasApiKey() {
        return !isBlank(apiKey);
    }

    /** Groq (API compatível com OpenAI). Plano gratuito com limite de requisições. */
    public record Groq(String baseUrl, String apiKey, String model, String reasoningEffort) {
        public Groq {
            baseUrl = isBlank(baseUrl) ? "https://api.groq.com/openai/v1" : baseUrl;
            model = isBlank(model) ? "openai/gpt-oss-20b" : model;
            reasoningEffort = isBlank(reasoningEffort) ? null : reasoningEffort.trim();
        }

        public boolean hasApiKey() {
            return !isBlank(apiKey);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
