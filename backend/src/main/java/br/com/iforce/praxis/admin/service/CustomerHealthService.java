package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.EmpresaHealthResponse;
import br.com.iforce.praxis.admin.model.CustomerHealthLevel;
import br.com.iforce.praxis.admin.model.EmpresaStatus;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula a saúde de retenção dos clientes comparando duas janelas consecutivas de uso.
 */
@Service
public class CustomerHealthService {

    private final EmpresaRepository empresaRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final int periodDays;
    private final double atRiskDropThreshold;
    private final long minBaselineCompletions;

    public CustomerHealthService(
            EmpresaRepository empresaRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            @Value("${praxis.retention.health-period-days:30}") int periodDays,
            @Value("${praxis.retention.at-risk-drop-threshold:0.30}") double atRiskDropThreshold,
            @Value("${praxis.retention.min-baseline-completions:3}") long minBaselineCompletions
    ) {
        this.empresaRepository = empresaRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.periodDays = periodDays;
        this.atRiskDropThreshold = atRiskDropThreshold;
        this.minBaselineCompletions = minBaselineCompletions;
    }

    /**
     * Retorna somente clientes em risco. As volumetrias são agregadas em lote para evitar N+1.
     */
    @Transactional(readOnly = true)
    public List<EmpresaHealthResponse> atRiskEmpresas(Instant now) {
        List<EmpresaEntity> empresas = empresaRepository.findByStatuses(List.of(EmpresaStatus.ATIVO));
        if (empresas.isEmpty()) {
            return List.of();
        }

        Instant currentStart = now.minus(periodDays, ChronoUnit.DAYS);
        Instant previousStart = now.minus(2L * periodDays, ChronoUnit.DAYS);
        List<String> empresaIds = empresas.stream().map(EmpresaEntity::getId).toList();

        Map<String, HealthWindow> windows = loadHealthWindows(
                empresaIds,
                previousStart,
                currentStart,
                now
        );
        Map<String, Instant> lastCompletedAt = loadLastCompletedAt(empresaIds);

        return empresas.stream()
                .map(empresa -> {
                    HealthWindow window = windows.getOrDefault(empresa.getId(), HealthWindow.EMPTY);
                    return toResponse(
                            empresa,
                            now,
                            previousStart,
                            currentStart,
                            window.current(),
                            window.previous(),
                            lastCompletedAt.get(empresa.getId())
                    );
                })
                .filter(health -> health.level() == CustomerHealthLevel.AT_RISK)
                .sorted(Comparator.comparing(EmpresaHealthResponse::dropPercent).reversed())
                .toList();
    }

    /**
     * Calcula a saúde de um único cliente. Mantido para consultas pontuais e testes determinísticos.
     */
    @Transactional(readOnly = true)
    public EmpresaHealthResponse healthFor(EmpresaEntity empresa, Instant now) {
        Instant currentStart = now.minus(periodDays, ChronoUnit.DAYS);
        Instant previousStart = now.minus(2L * periodDays, ChronoUnit.DAYS);

        long current = candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtBetween(
                empresa.getId(), AttemptStatus.COMPLETED, currentStart, now);
        long previous = candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtBetween(
                empresa.getId(), AttemptStatus.COMPLETED, previousStart, currentStart);
        Instant lastCompletedAt = candidateAttemptRepository
                .findLastFinishedAt(empresa.getId(), AttemptStatus.COMPLETED)
                .orElse(null);

        return toResponse(
                empresa,
                now,
                previousStart,
                currentStart,
                current,
                previous,
                lastCompletedAt
        );
    }

    private Map<String, HealthWindow> loadHealthWindows(
            List<String> empresaIds,
            Instant previousStart,
            Instant currentStart,
            Instant currentEnd
    ) {
        Map<String, HealthWindow> result = new HashMap<>();
        for (Object[] row : candidateAttemptRepository.summarizeHealthPeriods(
                empresaIds,
                AttemptStatus.COMPLETED,
                previousStart,
                currentStart,
                currentEnd
        )) {
            result.put(
                    (String) row[0],
                    new HealthWindow(number(row[1]), number(row[2]))
            );
        }
        return result;
    }

    private Map<String, Instant> loadLastCompletedAt(List<String> empresaIds) {
        Map<String, Instant> result = new HashMap<>();
        for (Object[] row : candidateAttemptRepository.findLastFinishedAtByEmpresaIds(
                empresaIds,
                AttemptStatus.COMPLETED
        )) {
            result.put((String) row[0], (Instant) row[1]);
        }
        return result;
    }

    private EmpresaHealthResponse toResponse(
            EmpresaEntity empresa,
            Instant now,
            Instant previousStart,
            Instant currentStart,
            long current,
            long previous,
            Instant lastCompletedAt
    ) {
        CustomerHealthLevel level;
        Double dropPercent;
        Integer healthScore;

        if (previous < minBaselineCompletions) {
            level = CustomerHealthLevel.NO_BASELINE;
            dropPercent = null;
            healthScore = null;
        } else {
            double drop = (double) (previous - current) / (double) previous;
            dropPercent = drop;
            double retentionRatio = Math.min(1.0, (double) current / (double) previous);
            healthScore = (int) Math.round(retentionRatio * 100);
            level = drop > atRiskDropThreshold
                    ? CustomerHealthLevel.AT_RISK
                    : CustomerHealthLevel.HEALTHY;
        }

        return new EmpresaHealthResponse(
                empresa.getId(),
                empresa.getName(),
                empresa.getTradeName(),
                empresa.getCorporateEmail(),
                empresa.getCommercialPlanType(),
                empresa.getStatus(),
                level,
                current,
                previous,
                dropPercent,
                healthScore,
                lastCompletedAt,
                currentStart,
                now,
                previousStart,
                currentStart
        );
    }

    private static long number(Object value) {
        return value == null ? 0L : ((Number) value).longValue();
    }

    private record HealthWindow(long current, long previous) {
        private static final HealthWindow EMPTY = new HealthWindow(0L, 0L);
    }
}
