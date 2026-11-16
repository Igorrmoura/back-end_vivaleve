package vivaleve.proj;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vivaleve.proj.dto.MoodLogDto;
import vivaleve.proj.exceptions.NotFoundException;
import vivaleve.proj.model.MoodLogEntity;
import vivaleve.proj.model.UserEntity;
import vivaleve.proj.model.enums.Activity;
import vivaleve.proj.model.enums.MoodType;
import vivaleve.proj.repository.MoodLogRepository;
import vivaleve.proj.repository.UserRepository;
import vivaleve.proj.service.MoodLogService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class MoodLogServiceOwnershipTest {

    private final MoodLogRepository repo = mock(MoodLogRepository.class);
    private final MoodLogService service = new MoodLogService(repo, mock(UserRepository.class));

    @Test
    void registroDeOutroUsuarioEhTratadoComo404(){
        UserEntity dono = new UserEntity();
        ReflectionTestUtils.setField(dono, "id", 99L);
        MoodLogEntity log = MoodLogEntity.builder().user(dono).build();
        when(repo.findById(50L)).thenReturn(Optional.of(log));

        var req = new MoodLogDto.MoodLogRequest(MoodType.values()[0], null, null);

        assertThrows(NotFoundException.class, () -> service.update(7L, 50L, req, null));
        assertThrows(NotFoundException.class, () -> service.delete(7L, 50L));

        verify(repo, never()).save(any());
        verify(repo, never()).delete(any());

    }

    @Test
    void atualizarSemAtividadesMantemAsAtuaisEComAtividadesSubstitui() {
        UserEntity dono = new UserEntity();
        ReflectionTestUtils.setField(dono, "id", 7L);
        MoodLogEntity log = MoodLogEntity.builder().user(dono)
                .activities(new HashSet<>(Set.of(Activity.WORK))).build();
        when(repo.findById(50L)).thenReturn(Optional.of(log));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        var semAtividades = service.update(7L, 50L, new MoodLogDto.MoodLogRequest(MoodType.BEM, "ok", null), null);
        assertThat(semAtividades.activities()).containsExactly(Activity.WORK);

        var comAtividades = service.update(7L, 50L,
                new MoodLogDto.MoodLogRequest(MoodType.ANSIOSO, null, Set.of(Activity.REST, Activity.FAMILY)), null);
        assertThat(comAtividades.activities()).containsExactlyInAnyOrder(Activity.REST, Activity.FAMILY);
        assertThat(comAtividades.mood()).isEqualTo(MoodType.ANSIOSO);
    }
}
