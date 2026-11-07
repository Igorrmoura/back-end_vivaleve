package vivaleve.proj.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vivaleve.proj.model.EntryEntity;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EntryRepository extends JpaRepository<EntryEntity, Long> {

    List<EntryEntity> findByUser_IdOrderByDateDesc(Long userId);

    List<EntryEntity> findByUser_IdAndDateBetweenOrderByDateAsc(Long userId, LocalDate start, LocalDate end);
}
