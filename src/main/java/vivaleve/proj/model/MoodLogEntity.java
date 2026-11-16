
package vivaleve.proj.model;

import jakarta.persistence.*;
import lombok.*;
import vivaleve.proj.model.enums.Activity;
import vivaleve.proj.model.enums.MoodType;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "registros_humor")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodLogEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MoodType mood;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "registro_humor_atividades", joinColumns = @JoinColumn(name = "registro_humor_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "atividade", nullable = false, length = 30)
    private Set<Activity> activities = new HashSet<>();

    @Column(length = 500)
    private String note;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;
}

