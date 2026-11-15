package vivaleve.proj.service;

import org.junit.jupiter.api.Test;
import vivaleve.proj.dto.HistoryDto;
import vivaleve.proj.repository.BreathingSessionRepository;
import vivaleve.proj.repository.MoodLogRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Resumo do histórico (/api/history/summary): agora conta as sessões de respiração do usuário. */
class HistoryServiceSummaryTest {

    private final MoodLogRepository moodLogs = mock(MoodLogRepository.class);
    private final BreathingSessionRepository breathing = mock(BreathingSessionRepository.class);
    private final HistoryService service = new HistoryService(moodLogs, breathing);

    @Test
    void resumoContaAsSessoesDeRespiracaoDoUsuario() {
        when(moodLogs.findDistinctDates(7L)).thenReturn(List.of());
        when(moodLogs.countByUserId(7L)).thenReturn(4L);
        when(breathing.countByUser_Id(7L)).thenReturn(3L);

        HistoryDto.SummaryResponse resumo = service.summary(7L);

        assertThat(resumo.totalRecords()).isEqualTo(4L);
        assertThat(resumo.breathingSessions()).isEqualTo(3L);
    }

    @Test
    void semSessoesDeRespiracaoOResumoMostraZero() {
        when(moodLogs.findDistinctDates(8L)).thenReturn(List.of());
        when(breathing.countByUser_Id(8L)).thenReturn(0L);

        assertThat(service.summary(8L).breathingSessions()).isZero();
    }
}
