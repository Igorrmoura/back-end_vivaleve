package vivaleve.proj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vivaleve.proj.dto.AuthDtos.AuthResponse;
import vivaleve.proj.dto.UserResponseDto;
import vivaleve.proj.model.UserEntity;
import vivaleve.proj.repository.UserRepository;
import vivaleve.proj.service.GoogleTokenVerifier.GoogleUser;

import java.time.Instant;
import java.util.Locale;

/**
 * Regras de negócio de autenticação.
 *
 * <p>ALTERADO (OAuth2 Google): o login e o cadastro com email + senha foram removidos
 * (junto com o BCrypt). O único jeito de entrar é o botão do Google: o front manda o ID Token
 * para {@code POST /v1/auth/google} → {@link #loginWithGoogle(String)}.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li><b>Login/cadastro com Google</b>: se o usuário ainda não existir, cria a conta na hora —
 *       o primeiro login É o cadastro.</li>
 *   <li><b>Vínculo de contas antigas</b>: quem já tinha conta com email + senha entra com o
 *       Google do mesmo email e mantém todos os seus registros.</li>
 *   <li><b>Emissão de JWT</b> (sem mudança): o token da API carrega o id interno do usuário no
 *       {@code sub} e o email em uma claim; expira após {@code jwt.expiration-seconds}.
 *       O token do Google só é usado uma vez, aqui no login — nas demais rotas vale o JWT da API.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Identifica quem emitiu o token (claim "iss"). */
    private static final String ISSUER = "vivaleve-api";
    private static final String TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final JwtEncoder jwtEncoder;

    /** Tempo de vida do token em segundos (padrão: 24h). */
    @Value("${jwt.expiration-seconds:86400}")
    private long expirationSeconds;

    /**
     * Login com Google ({@code POST /v1/auth/google}, usado pelo botão do Google no front).
     *
     * <p>Valida o ID Token com o {@link GoogleTokenVerifier} (assinatura, aud, iss, exp,
     * email verificado) e segue para o login/cadastro comum em {@link #loginWithGoogle(GoogleUser)}.</p>
     *
     * @param idToken o {@code credential} entregue pelo botão "Entrar com Google"
     * @throws ResponseStatusException 401 se o token do Google for inválido;
     *                                 409 se o email já pertencer a OUTRA conta Google
     */
    @Transactional
    public AuthResponse loginWithGoogle(String idToken) {
        return loginWithGoogle(googleTokenVerifier.verify(idToken));
    }

    /**
     * NOVO — Login e cadastro a partir de uma conta Google.
     *
     * <p>Os dados da conta já chegam validados pelo {@link GoogleTokenVerifier}, então aqui só
     * existe regra de negócio:</p>
     * <ol>
     *   <li>Procura o usuário pelo {@code sub} do Google → login normal.</li>
     *   <li>Se não achar, procura pelo email → conta antiga (email + senha): vincula o {@code sub} a ela.</li>
     *   <li>Se também não achar → cadastro: cria um usuário novo.</li>
     *   <li>Atualiza nome/foto (podem ter mudado no Google) e devolve o JWT da API.</li>
     * </ol>
     *
     * @param google dados de uma conta Google JÁ VALIDADA (nunca montar a partir de dados do cliente)
     * @throws ResponseStatusException 409 se o email já pertencer a OUTRA conta Google
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleUser google) {
        String email = normalizeEmail(google.email());

        UserEntity user = userRepository.findByGoogleSub(google.sub())
                .orElseGet(() -> linkOrCreate(google.sub(), email));

        // Mantém o perfil sincronizado com o Google a cada login.
        user.setEmail(email);
        user.setName(google.name());
        user.setPictureUrl(google.pictureUrl());
        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    /**
     * Feature: vínculo de conta antiga ou criação de conta nova.
     *
     * <p>Só chega aqui quando nenhum usuário tem esse {@code sub}. Se existir um usuário com o
     * mesmo email e SEM {@code sub}, é uma conta antiga (email + senha) — o Google garante que
     * o email é verificado, então é seguro vinculá-la. Se o usuário com esse email já tiver
     * OUTRO {@code sub}, é outra conta Google (o email trocou de dono) e o acesso é negado.</p>
     */
    private UserEntity linkOrCreate(String googleSub, String email) {
        return userRepository.findByEmail(email)
                .map(existing -> {
                    if (existing.getGoogleSub() != null) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Este email já está vinculado a outra conta Google");
                    }
                    existing.setGoogleSub(googleSub);
                    return existing;
                })
                .orElseGet(() -> UserEntity.builder()
                        .googleSub(googleSub)
                        .email(email)
                        .build());
    }

    /** Gera o JWT da API e monta o JSON de resposta. */
    private AuthResponse buildAuthResponse(UserEntity user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(expirationSeconds);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId())) // quem é o dono do token (id interno, não o sub do Google)
                .claim("email", user.getEmail())
                .build();

        // O header precisa declarar HS256, pois o padrão do encoder seria RS256 (chave assimétrica).
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        return new AuthResponse(token, TOKEN_TYPE, expiresAt, UserService.toDto(user));
    }

    /** Emails são comparados sem diferenciar maiúsculas/minúsculas nem espaços nas pontas. */
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
