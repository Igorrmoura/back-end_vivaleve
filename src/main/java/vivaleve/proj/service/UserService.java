package vivaleve.proj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import vivaleve.proj.dto.UserDto;
import vivaleve.proj.model.UserEntity;
import vivaleve.proj.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void criarUser(UserDto userDto) {
        UserEntity user = userRepository.findByEmail(userDto.getEmail())
                .orElse(null);

    }
}
