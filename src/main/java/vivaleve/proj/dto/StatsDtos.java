package vivaleve.proj.dto;

import vivaleve.proj.model.enums.Mood;

import java.time.LocalDate;
import java.util.List;

public final class StatsDtos {

    private StatsDtos() {
    }

    public record StatsSummary(
            int days,
            LocalDate startDate,
            LocalDate endDate,
            long totalEntries,
            Mood predominantMood,
            int currentStreak,
            int longestStreak,
            List<MoodEvolutionPoint> evolution,
            List<MoodDistributionItem> distribution
    ) {
    }

    public record MoodEvolutionPoint(
            LocalDate date,
            double averageScore
    ) {
    }

    public record MoodDistributionItem(
            Mood mood,
            long count,
            double percentage
    ) {
    }
}
