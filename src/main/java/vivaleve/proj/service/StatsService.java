package vivaleve.proj.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import vivaleve.proj.dto.StatsDtos.MoodDistributionItem;
import vivaleve.proj.dto.StatsDtos.MoodEvolutionPoint;
import vivaleve.proj.dto.StatsDtos.StatsSummary;
import vivaleve.proj.model.EntryEntity;
import vivaleve.proj.model.enums.Mood;
import vivaleve.proj.repository.EntryRepository;
import vivaleve.proj.repository.UserRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private static final Set<Integer> PERIODOS_PERMITIDOS = Set.of(7, 30);

    private final EntryRepository entryRepository;
    private final UserRepository userRepository;

    /**
     * Gera o resumo de estatísticas do usuário nos últimos 7 ou 30 dias
     * (evolução, distribuição, humor predominante e streaks).
     */
    @Transactional(readOnly = true)
    public StatsSummary gerarResumo(Long userId, int days) {
        if (!PERIODOS_PERMITIDOS.contains(days)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O parâmetro days deve ser 7 ou 30");
        }
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }

        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.minusDays(days - 1L);

        List<EntryEntity> entries =
                entryRepository.findByUser_IdAndDateBetweenOrderByDateAsc(userId, inicio, hoje);

        Map<Mood, Long> contagem = contarPorHumor(entries);

        return new StatsSummary(
                days,
                inicio,
                hoje,
                entries.size(),
                calcularPredominante(contagem),
                calcularStreakAtual(entryRepository.findDistinctDatesByUserId(userId), hoje),
                calcularMaiorStreak(entries),
                calcularEvolucao(entries),
                calcularDistribuicao(contagem, entries.size())
        );
    }

    /** Média do humor por dia (GREAT=5 ... AWFUL=1). Dias sem registro ficam de fora. */
    private List<MoodEvolutionPoint> calcularEvolucao(List<EntryEntity> entries) {
        Map<LocalDate, Double> mediaPorDia = entries.stream()
                .collect(Collectors.groupingBy(
                        EntryEntity::getDate,
                        TreeMap::new,
                        Collectors.averagingInt(e -> e.getMood().getScore())));

        return mediaPorDia.entrySet().stream()
                .map(e -> new MoodEvolutionPoint(e.getKey(), arredondar(e.getValue())))
                .toList();
    }

    /** Quantidade e porcentagem de cada humor, incluindo os que tiveram zero registros. */
    private List<MoodDistributionItem> calcularDistribuicao(Map<Mood, Long> contagem, long total) {
        return contagem.entrySet().stream()
                .map(e -> new MoodDistributionItem(
                        e.getKey(),
                        e.getValue(),
                        total == 0 ? 0.0 : arredondar(e.getValue() * 100.0 / total)))
                .toList();
    }

    /** Humor mais frequente. Em caso de empate, vence o humor melhor (ordem do enum). */
    private Mood calcularPredominante(Map<Mood, Long> contagem) {
        return contagem.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Dias seguidos com registro até hoje. Se o usuário ainda não registrou hoje,
     * conta a partir de ontem, para a sequência não "quebrar" antes do dia acabar.
     */
    private int calcularStreakAtual(List<LocalDate> datas, LocalDate hoje) {
        Set<LocalDate> diasComRegistro = new HashSet<>(datas);
        LocalDate dia = diasComRegistro.contains(hoje) ? hoje : hoje.minusDays(1);

        int streak = 0;
        while (diasComRegistro.contains(dia)) {
            streak++;
            dia = dia.minusDays(1);
        }
        return streak;
    }

    /** Maior sequência de dias seguidos com registro dentro do período. */
    private int calcularMaiorStreak(List<EntryEntity> entries) {
        List<LocalDate> datas = entries.stream()
                .map(EntryEntity::getDate)
                .distinct()
                .sorted()
                .toList();

        int maior = 0;
        int atual = 0;
        LocalDate anterior = null;
        for (LocalDate data : datas) {
            atual = (anterior != null && anterior.plusDays(1).equals(data)) ? atual + 1 : 1;
            maior = Math.max(maior, atual);
            anterior = data;
        }
        return maior;
    }

    /** Conta quantos registros existem de cada humor, começando todos em zero. */
    private Map<Mood, Long> contarPorHumor(List<EntryEntity> entries) {
        Map<Mood, Long> contagem = new EnumMap<>(Mood.class);
        for (Mood mood : Mood.values()) {
            contagem.put(mood, 0L);
        }
        entries.forEach(e -> contagem.merge(e.getMood(), 1L, Long::sum));
        return contagem;
    }

    /** Arredonda o valor para 2 casas decimais. */
    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
