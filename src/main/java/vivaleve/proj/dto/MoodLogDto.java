package vivaleve.proj.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import vivaleve.proj.model.enums.Activity;
import vivaleve.proj.model.enums.MoodType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public class MoodLogDto {

    public record MoodLogRequest(
            @NotNull(message = "Escolha uma emoção") MoodType mood,
            @Size(max = 500, message = "A nota deve ter no máximo 500 caracteres") String note,
            Set<Activity> activities   // opcional; no PUT, null = mantém as atividades atuais
    ) {

    }

    public record MoodLogResponse(
            Long id,
            MoodType mood,
            Set<Activity> activities,
            String note,
            String imageUrl,
            LocalDate logDate,
            LocalDateTime createdAt,
            UserResponseDto user      // novo
    ) {
    }

    public record MoodStatsResponse(Map<MoodType, Long> counts, long total) {
    }
}
