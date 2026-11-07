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
    private String nome;
    @NotBlank
    private String email;
}
