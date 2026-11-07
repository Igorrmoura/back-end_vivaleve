package vivaleve.proj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserDto {

    @NotBlank
    private String email;
    @NotBlank
    private String password;
}
