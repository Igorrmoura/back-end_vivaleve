package vivaleve.proj.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Usuário da aplicação.
 *
 * <p>ALTERADO (OAuth2 Google): a conta agora é criada e acessada exclusivamente pelo Google.
 * O campo {@code password} foi removido — a API não guarda mais senha nenhuma.</p>
 */
@Table(name = "usuarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class UserEntity extends BaseEntity {

    /**
     * Email da conta Google (já verificado pelo Google).
     * Continua único: é usado para vincular contas antigas (email + senha) no primeiro login Google.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * NOVO — Identificador permanente da conta Google (claim {@code sub} do ID Token).
     *
     * <p>É ele que identifica o usuário, e não o email: o email de uma conta Google pode mudar,
     * o {@code sub} nunca muda. Fica nullable no banco só para que as linhas antigas
     * (criadas com email + senha) continuem válidas até o primeiro login Google, quando são vinculadas.</p>
     */
    @Column(name = "google_sub", unique = true, length = 255)
    private String googleSub;

    /** NOVO — Nome exibido na conta Google (claim {@code name}). Atualizado a cada login. */
    @Column(length = 100)
    private String name;

    /** NOVO — URL da foto de perfil do Google (claim {@code picture}). Atualizada a cada login. */
    @Column(name = "picture_url", length = 512)
    private String pictureUrl;
}
