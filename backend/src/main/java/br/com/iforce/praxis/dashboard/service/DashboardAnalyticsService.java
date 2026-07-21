package br.com.iforce.praxis.dashboard.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse;
import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse.ActivityPoint;
import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse.ParticipationSummary;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula os indicadores visuais do dashboard sem carregar registros detalhados de candidatos.
 */
@Service
public class DashboardAnalyticsService {

    private static final int PERIOD_DAYS = 30;
    private static final ZoneId DASHBOARD_ZONE = ZoneOffset.UTC;

    private final CurrentEmpresaService currentEmpresaService;
    private final CandidateAttemptRepository candidateAttemptRepository;

    public DashboardAnalyticsService(
            CurrentEmpresaService currentEmpresaService,
            CandidateAttemptRepository candidateAttemptRepository
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.candidateAttemptRepository = candidateAttemptRepository;
    }

    /**
     * Retorna o funil, as taxas e a atividade diária da empresa autenticada.
     *
     * @return indicadores analíticos do dashboard
     */
    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse getAnalytics() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        Instant generatedAt = Instant.now();
        LocalDate today = LocalDate.ofInstant(generatedAt, DASHBOARD_ZONE);
        LocalDate firstDate = today.minusDays(PERIOD_DAYS - 1L);
        Instant periodStart = firstDate.atStartOfDay(DASHBOARD_ZONE).toInstant();

        Specification<CandidateAttemptEntity> companyScope = belongsToEmpresa(empresaId);

        long total = candidateAttemptRepository.count(companyScope);
        long notStarted = candidateAttemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.NOT_STARTED);
        long inProgress = candidateAttemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.IN_PROGRESS);
        long completed = candidateAttemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.COMPLETED);
        long abandoned = candidateAttemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.ABANDONED);
        long expired = candidateAttemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.EXPIRED);

        long startedFromTimestamp = candidateAttemptRepository.count(
                companyScope.and(startedAtIsPresent())
        );
        long started = Math.max(startedFromTimestamp, completed + abandoned);
        long resolved = completed + abandoned + expired;

        List<CandidateAttemptEntity> createdInPeriod = candidateAttemptRepository.findAll(
                companyScope.and(createdAtOnOrAfter(periodStart)),
                Sort.by(Sort.Direction.ASC, "createdAt")
        );
        List<CandidateAttemptEntity> finishedInPeriod = candidateAttemptRepository.findAll(
                companyScope.and(finishedAtOnOrAfter(periodStart)),
                Sort.by(Sort.Direction.ASC, "finishedAt")
        );

        Map<LocalDate, ActivityAccumulator> activityByDate = initializeActivity(firstDate);
        createdInPeriod.forEach(attempt -> incrementCreated(activityByDate, attempt));
        finishedInPeriod.forEach(attempt -> incrementFinished(activityByDate, attempt));

        Double averageScoreLast30Days = averageCompletedScore(finishedInPeriod);
        ParticipationSummary participations = new ParticipationSummary(
                total,
                started,
                notStarted,
                inProgress,
                completed,
                abandoned,
                expired,
                percentage(completed, resolved),
                percentage(abandoned + expired, resolved),
                averageScoreLast30Days
        );

        List<ActivityPoint> activity = activityByDate.entrySet().stream()
                .map(entry -> new ActivityPoint(
                        entry.getKey(),
                        entry.getValue().created,
                        entry.getValue().completed,
                        entry.getValue().abandoned
                ))
                .toList();

        return new DashboardAnalyticsResponse(generatedAt, PERIOD_DAYS, participations, activity);
    }

    private static Specification<CandidateAttemptEntity> belongsToEmpresa(String empresaId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("empresaId"), empresaId);
    }

    private static Specification<CandidateAttemptEntity> startedAtIsPresent() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("startedAt"));
    }

    private static Specification<CandidateAttemptEntity> createdAtOnOrAfter(Instant periodStart) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(
                root.<Instant>get("createdAt"),
                periodStart
        );
    }

    private static Specification<CandidateAttemptEntity> finishedAtOnOrAfter(Instant periodStart) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(
                root.<Instant>get("finishedAt"),
                periodStart
        );
    }

    private static Map<LocalDate, ActivityAccumulator> initializeActivity(LocalDate firstDate) {
        Map<LocalDate, ActivityAccumulator> activity = new LinkedHashMap<>();
        for (int offset = 0; offset < PERIOD_DAYS; offset++) {
            activity.put(firstDate.plusDays(offset), new ActivityAccumulator());
        }
        return activity;
    }

    private static void incrementCreated(
            Map<LocalDate, ActivityAccumulator> activityByDate,
            CandidateAttemptEntity attempt
    ) {
        LocalDate date = LocalDate.ofInstant(attempt.getCreatedAt(), DASHBOARD_ZONE);
        ActivityAccumulator accumulator = activityByDate.get(date);
        if (accumulator != null) {
            accumulator.created++;
        }
    }

    private static void incrementFinished(
            Map<LocalDate, ActivityAccumulator> activityByDate,
            CandidateAttemptEntity attempt
    ) {
        if (attempt.getFinishedAt() == null) {
            return;
        }
        LocalDate date = LocalDate.ofInstant(attempt.getFinishedAt(), DASHBOARD_ZONE);
        ActivityAccumulator accumulator = activityByDate.get(date);
        if (accumulator == null) {
            return;
        }
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            accumulator.completed++;
        } else if (attempt.getStatus() == AttemptStatus.ABANDONED || attempt.getStatus() == AttemptStatus.EXPIRED) {
            accumulator.abandoned++;
        }
    }

    private static Double averageCompletedScore(List<CandidateAttemptEntity> attempts) {
        return attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED)
                .map(CandidateAttemptEntity::getScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private static double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 1000.0) / denominator) / 10.0;
    }

    private static final class ActivityAccumulator {
        private long created;
        private long completed;
        private long abandoned;
    }
}
