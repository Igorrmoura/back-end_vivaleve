package vivaleve.proj.model.enums;

/** Quem escreveu a mensagem. Os valores em minúsculo são os aceitos pela API do Ollama. */
public enum AiMessageRole {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    private final String apiValue;

    AiMessageRole(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }
}
