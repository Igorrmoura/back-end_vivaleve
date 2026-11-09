package vivaleve.proj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vivaleve.proj.dto.AuthDtos.AuthResponse;
import vivaleve.proj.dto.AuthDtos.LoginRequest;
import vivaleve.proj.dto.AuthDtos.RegisterRequest;
import vivaleve.proj.dto.UserResponseDto;
import vivaleve.proj.model.UserEntity;
import vivaleve.proj.repository.UserRepository;

import java.time.Instant;
import java.util.Locale;

/**
 * Regras de negócio de autenticação.
 *
 * <p>Features:</p>
 * <ul>
 *   <li><b>Registro</b>: cria o usuário guardando a senha com hash BCrypt (nunca em texto puro)
 *       e já devolve um token, para o front não precisar chamar o login em seguida.</li>
 *   <li><b>Login</b>: confere email + senha e devolve um JWT novo.</li>
 *   <li><b>Emissão de JWT</b>: o token carrega o id do usuário no {@code sub} e o email
 *       em uma claim; expira após {@code jwt.expiration-seconds}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Identifica quem emitiu o token (claim "iss"). */
    private static final String ISSUER = "vivaleve-api";
    private static final String TOKEN_TYPE = "Bearer";

    /**
     * Mensagem única para "email não existe" e "senha errada": assim quem tenta logar
     * não descobre quais emails estão cadastrados (evita enumeração de usuários).
     */
    private static final String INVALID_CREDENTIALS = "Email ou senha inválidos";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    /** Tempo de vida do token em segundos (padrão: 24h). */
    @Value("${jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    /**
     * Cadastra um novo usuário.
     *
     * @throws ResponseStatusException 409 (CONFLICT) se o email já estiver em uso
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário já cadastrado com este email");
        }

        UserEntity user = userRepository.save(UserEntity.builder()
                .email(email)
                // O hash BCrypt tem 60 caracteres e cabe na coluna "password" (64).
                .password(passwordEncoder.encode(request.password()))
                .build());

        return buildAuthResponse(user);
    }

    /**
     * Autentica o usuário.
     *
     * @throws ResponseStatusException 401 (UNAUTHORIZED) se o email não existir ou a senha não bater
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS));

        // matches() aplica o hash na senha digitada e compara com o hash salvo.
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS);
        }

        return buildAuthResponse(user);
    }

    /** Gera o JWT e monta o JSON de resposta. */
    private AuthResponse buildAuthResponse(UserEntity user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId())) // quem é o dono do token
                .claim("email", user.getEmail())
                .build();

        // O header precisa declarar HS256, pois o padrão do encoder seria RS256 (chave assimétrica).
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        UserResponseDto userDto = UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();

        return new AuthResponse(token, TOKEN_TYPE, expiresAt, userDto);
    }

    /** Emails são comparados sem diferenciar maiúsculas/minúsculas nem espaços nas pontas. */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
