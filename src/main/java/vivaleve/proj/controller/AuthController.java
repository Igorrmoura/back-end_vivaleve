package vivaleve.proj.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vivaleve.proj.dto.AuthDtos.AuthResponse;
import vivaleve.proj.dto.AuthDtos.LoginRequest;
import vivaleve.proj.dto.AuthDtos.RegisterRequest;
import vivaleve.proj.service.AuthService;

/**
 * Endpoints públicos de autenticação (liberados no {@code SecurityConfig}).
 *
 * <p>O controller só recebe o JSON, dispara a validação ({@code @Valid}) e delega
 * para o {@link AuthService}. Ambos os endpoints devolvem um {@link AuthResponse}
 * com o JWT que o front deve mandar nas próximas requisições:</p>
 *
 * <pre>Authorization: Bearer &lt;token&gt;</pre>
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final AuthService authService;

    /**
     * POST /v1/auth/register — cria a conta e já devolve o token.
     * Respostas: 201 (criado), 400 (dados inválidos), 409 (email já cadastrado).
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    /**
     * POST /v1/auth/login — troca email + senha por um token.
     * Respostas: 200 (ok), 400 (dados inválidos), 401 (credenciais incorretas).
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
