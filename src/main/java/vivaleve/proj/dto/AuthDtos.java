package vivaleve.proj.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

/**
 * DTOs (formato do JSON) usados pelos endpoints de autenticação.
 *
 * <p>Ficam agrupados como {@code record}s dentro de uma única classe, no mesmo
 * padrão de {@link EntryDtos}. Records são imutáveis e já geram construtor,
 * getters, equals/hashCode e toString.</p>
 *
 * <p>ALTERADO (OAuth2 Google): {@code RegisterRequest} e {@code LoginRequest} (email + senha)
 * foram removidos. Login e cadastro agora usam apenas {@link GoogleLoginRequest}.</p>
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /**
     * NOVO — Corpo de POST /v1/auth/google.
     *
     * <p>{@code credential} é o ID Token (um JWT) que o botão "Entrar com Google"
     * (Google Identity Services) entrega para o front no callback. O front só repassa
     * esse valor; quem decide se ele é válido é o back-end ({@code GoogleTokenVerifier}).</p>
     *
     * <p>Propositalmente não há campos de email/nome aqui: esses dados são lidos de dentro
     * do token assinado pelo Google, nunca de algo que o cliente possa digitar.</p>
     */
    public record GoogleLoginRequest(
            @NotBlank(message = "O credential do Google é obrigatório")
            String credential
    ) {
    }

    /**
     * Resposta do login.
     *
     * @param token     JWT da própria API (não é o token do Google) que o front deve enviar
     *                  no header {@code Authorization: Bearer <token>}
     * @param tokenType sempre "Bearer"
     * @param expiresAt instante em que o token deixa de ser válido
     * @param user      dados públicos do usuário autenticado
     */
    public record AuthResponse(
            String token,
            String tokenType,
            Instant expiresAt,
            UserResponseDto user
    ) {
    }
}
