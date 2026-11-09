package vivaleve.proj.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTOs (formato do JSON) usados pelos endpoints de autenticação.
 *
 * <p>Ficam agrupados como {@code record}s dentro de uma única classe, no mesmo
 * padrão de {@link EntryDtos}. Records são imutáveis e já geram construtor,
 * getters, equals/hashCode e toString.</p>
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    /**
     * Corpo de POST /v1/auth/register.
     * As anotações do Bean Validation são verificadas pelo {@code @Valid} no controller;
     * se alguma falhar, o Spring devolve 400 antes de chegar no service.
     */
    public record RegisterRequest(
            @NotBlank(message = "O email é obrigatório")
            @Email(message = "Informe um email válido")
            @Size(max = 100, message = "O email deve ter no máximo 100 caracteres")
            String email,

            @NotBlank(message = "A senha é obrigatória")
            @Size(min = 8, max = 64, message = "A senha deve ter entre 8 e 64 caracteres")
            String password
    ) {
    }

    /**
     * Corpo de POST /v1/auth/login.
     * Aqui não validamos tamanho de senha: qualquer senha errada simplesmente resulta em 401.
     */
    public record LoginRequest(
            @NotBlank(message = "O email é obrigatório")
            @Email(message = "Informe um email válido")
            String email,

            @NotBlank(message = "A senha é obrigatória")
            String password
    ) {
    }

    /**
     * Resposta de registro e login.
     *
     * @param token     JWT assinado que o front deve enviar no header {@code Authorization: Bearer <token>}
     * @param tokenType sempre "Bearer"
     * @param expiresAt instante em que o token deixa de ser válido
     * @param user      dados públicos do usuário autenticado (nunca inclui a senha)
     */
    public record AuthResponse(
            String token,
            String tokenType,
            Instant expiresAt,
            UserResponseDto user
    ) {
    }
}
