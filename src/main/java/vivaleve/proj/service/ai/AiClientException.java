package vivaleve.proj.service.ai;

/** Falha ao falar com o provedor de IA. O AiService trata e devolve a resposta de fallback. */
public class AiClientException extends RuntimeException {

    public AiClientException(String message) {
        super(message);
    }

    public AiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
