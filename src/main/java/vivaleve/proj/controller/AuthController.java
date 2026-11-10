package vivaleve.proj.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vivaleve.proj.dto.AuthDtos.AuthResponse;
import vivaleve.proj.dto.AuthDtos.GoogleLoginRequest;
import vivaleve.proj.service.AuthService;

/**
 * Endpoint público de autenticação (liberado no {@code SecurityConfig}).
 *
 * <p>ALTERADO (OAuth2 Google): {@code POST /v1/auth/register} e {@code POST /v1/auth/login}
 * (email + senha) foram removidos. Login e cadastro são feitos exclusivamente com a conta Google.</p>
 *
 * <p>O endpoint devolve um {@link AuthResponse} com o JWT da API que o front deve mandar
 * nas próximas requisições:</p>
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
     * NOVO — POST /v1/auth/google — troca o ID Token do Google por um JWT da API.
     *
     * <p>Serve tanto para login quanto para cadastro: se for o primeiro acesso dessa conta
     * Google, o usuário é criado automaticamente.</p>
     *
     * <p>Corpo: {@code { "credential": "<ID Token recebido do botão Entrar com Google>" }}</p>
     * <p>Respostas: 200 (ok), 400 (credential vazio), 401 (token do Google inválido ou email
     * não verificado), 409 (email já vinculado a outra conta Google), 503 (Google indisponível).</p>
     */
    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest request) {
        return authService.loginWithGoogle(request.credential());
    }
}
