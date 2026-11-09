package vivaleve.proj.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Configuração de segurança da API.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Fornecer o {@link PasswordEncoder} (BCrypt) usado para guardar senhas com hash.</li>
 *   <li>Fornecer o {@link JwtEncoder} (gera tokens no login/registro) e o {@link JwtDecoder}
 *       (valida o token que chega no header {@code Authorization: Bearer ...}).</li>
 *   <li>Definir quais rotas são públicas e deixar a API stateless (sem sessão/cookie).</li>
 * </ul>
 *
 * <p>Os tokens são assinados com HMAC-SHA256 usando a chave secreta {@code jwt.secret}.
 * A mesma chave assina e valida, então ela nunca deve ir para o repositório.</p>
 */
@Configuration
@EnableWebSecurity // liga o Spring Security explicitamente (também necessário nos testes de slice)
public class SecurityConfig {

    /** HS256 exige uma chave de no mínimo 256 bits (32 bytes). */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;

    public SecurityConfig(@Value("${jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            // Falha logo na inicialização em vez de gerar tokens inseguros.
            throw new IllegalStateException(
                    "jwt.secret (JWT_SECRET) deve ter pelo menos " + MIN_SECRET_BYTES + " bytes");
        }
        this.secretKey = new SecretKeySpec(bytes, "HmacSHA256");
    }

    /**
     * Regras HTTP:
     * <ul>
     *   <li>CSRF desligado: a API não usa cookies de sessão, só token no header.</li>
     *   <li>Sessão STATELESS: cada requisição se autentica sozinha pelo JWT.</li>
     *   <li>{@code /v1/auth/**} é sempre público (é onde o usuário obtém o token).</li>
     *   <li>{@code /error} é público: quando um controller lança erro (ex.: 409 no registro),
     *       o Spring encaminha internamente para {@code /error} para montar o JSON de erro.
     *       Se essa rota exigisse token, um 409/400 numa rota pública viraria 401.</li>
     *   <li><b>Qualquer outra rota exige um JWT válido</b> ({@code authenticated()}).
     *       Sem token, com token adulterado ou expirado → 401.</li>
     *   <li>{@code oauth2ResourceServer().jwt()}: lê o header {@code Authorization: Bearer ...}
     *       e valida o token com o {@link JwtDecoder} abaixo.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v1/auth/**", "/error").permitAll()
                        // ALTERADO: antes era permitAll(); agora todas as outras rotas exigem login.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth
                        .bearerTokenResolver(bearerTokenResolver())
                        .jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * Decide de onde ler o Bearer token de cada requisição.
     *
     * <p>Nas rotas {@code /v1/auth/**} o header {@code Authorization} é ignorado: quem está
     * fazendo login ou cadastro ainda não tem token válido. Sem isso, um token expirado,
     * gerado com outro {@code JWT_SECRET} ou um placeholder vazio (ex.: {@code {{token}}}
     * no Postman) faria o login responder 401 antes mesmo de chegar no controller.</p>
     */
    private BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        return request -> request.getRequestURI().startsWith(request.getContextPath() + "/v1/auth/")
                ? null
                : defaultResolver.resolve(request);
    }

    /** BCrypt gera um hash com "salt" aleatório; o mesmo texto gera hashes diferentes a cada vez. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Assina os JWTs emitidos pelo AuthService. */
    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    /** Valida assinatura e expiração dos JWTs recebidos nas requisições. */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
