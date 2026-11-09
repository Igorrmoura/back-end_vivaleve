package vivaleve.proj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vivaleve.proj.dto.UserResponseDto;
import vivaleve.proj.model.UserEntity;
import vivaleve.proj.repository.UserRepository;

/**
 * Regras de negócio do perfil do usuário.
 *
 * <p>ALTERADO: {@code criarUser} (senha sem hash) e {@code listarUsers} (expunha todos os
 * usuários) foram removidos. O cadastro agora é responsabilidade do {@link AuthService}.</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    /**
     * Devolve os dados públicos do usuário autenticado.
     * 404 se a conta foi apagada mas o token ainda não expirou.
     */
    @Transactional(readOnly = true)
    public UserResponseDto buscarPerfil(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado"));

        // Converte para DTO: só id e email saem no JSON, o hash da senha fica no servidor.
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .build();
    }
}
