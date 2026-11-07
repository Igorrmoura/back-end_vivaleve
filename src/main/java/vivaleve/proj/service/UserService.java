package vivaleve.proj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vivaleve.proj.dto.UserDto;
import vivaleve.proj.model.UserEntity;
import vivaleve.proj.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    @Autowired
    private final UserRepository userRepository;

    public UserEntity criarUser(UserDto userDto) {
        UserEntity entity = new UserEntity();

        entity.setEmail(userDto.getEmail());
        entity.setPassword(userDto.getPassword());

        return userRepository.save(entity);

    }

    public List<UserEntity> listarUsers() {
        return userRepository.findAll();
    }
}
