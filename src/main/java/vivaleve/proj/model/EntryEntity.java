package vivaleve.proj.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vivaleve.proj.model.enums.Activity;
import vivaleve.proj.model.enums.Mood;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Table(name = "registros")
@Getter
@Setter
@NoArgsConstructor
@Entity
public class EntryEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mood mood;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "registro_atividades", joinColumns = @JoinColumn(name = "registro_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "atividade", nullable = false, length = 30)
    private Set<Activity> activities = new HashSet<>();

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private UserEntity user;
}
