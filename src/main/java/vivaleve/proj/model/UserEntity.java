package vivaleve.proj.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;


@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@Entity
public class UserEntity extends BaseEntity {


        @Column(nullable = false, unique = true, length = 255)
        private String email;

        @Column(name = "password", nullable = false)
        private String password;


    }

