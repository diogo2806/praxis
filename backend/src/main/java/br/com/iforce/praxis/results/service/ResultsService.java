package br.com.iforce.praxis.results.service;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.candidate.dto.RegisterDispositionRequest;
import br.com.iforce.praxis.candidate.service.CandidateDispositionService;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.results.dto.RegisterResultDecisionRequest;
import br.com.iforce.praxis.results.dto.ResultDetailResponse;
import br.com.iforce.praxis.results.dto.ResultListItemResponse;
import br.com.iforce.praxis.results.dto.ResultsPageResponse;
import br.com.iforce.praxis.results.dto.ResultsSummaryResponse;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;
import br.com.iforce.praxis.audit.service.AuditEventService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResultsService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationRepository simulationRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final AuditEventService auditEventService;
    private final CandidateDispositionService candidateDispositionService;
    private final CurrentTenantService currentTenantService;
    private final ObjectMapper objectMapper;

    public ResultsService(
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationRepository simulationRepository,
            SimulationCatalogService simulationCatalogService,
            AuditEventService auditEventService,
            CandidateDispositionService candidateDispositionService,
            CurrentTenantService currentTenantService,
            ObjectMapper objectMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationRepository = simulationRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.auditEventService = auditEventService;
        this.candidateDispositionService = candidateDispositionService;
        this.currentTenantService = currentTenantService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ResultsPageResponse list(
            String search,
            String simulationId,
            AttemptStatus status,
            String integrationProvider,
            Instant periodStart,
            Instant periodEnd,
            int page,
            int size
    ) {
        String tenantId = currentTenantService.requiredTenantId();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Specification<CandidateAttemptEntity> spec = buildSpec(
                tenantId,
                search,
                simulationId,
                status,
                integrationProvider,
                periodStart,
                periodEnd
        );

        Page<CandidateAttemptEntity> resultPage = candidateAttemptRepository.findAll(
                spec,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<CandidateAttemptEntity> filtered = candidateAttemptRepository.findAll(spec);
        Map<String, String> simulationTitles = simulationTitles(tenantId);

        return new ResultsPageResponse(
                resultPage.getContent().stream()
                        .map(attempt -> toListItem(attempt, simulationTitles))
                        .toList(),
                summary(filtered),
                resultPage.getNumber(),
                resultPage.getSize(),
                resultPage.getTotalElements(),
                resultPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ResultDetailResponse get(String attemptId) {
        String tenantId = currentTenantService.requiredTenantId();
        CandidateAttemptEntity attempt = candidateAttemptRepository.findByTenantIdAndId(tenantId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resultado nÃ£o encontrado."));
        PublishedSimulation simulation = resolveSimulation(attempt, tenantId).orElse(null);
        String simulationTitle = simulation == null
                ? simulationRepository.findByTenantIdAndId(tenantId, attempt.getSimulationId())
                        .map(SimulationEntity::getName)
                        .orElse("AvaliaÃ§Ã£o")
                : simulation.name();
        List<AuditEventResponse> auditTrail = auditEventService.listCandidateAttemptEvents(attemptId);

        return new ResultDetailResponse(
                attempt.getId(),
                new ResultDetailResponse.Candidate(attempt.getCandidateName(), attempt.getCandidateEmail(), null),
                new ResultDetailResponse.Simulation(
                        attempt.getSimulationId(),
                        simulationTitle,
                        attempt.getSimulationVersionNumber()
                ),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getScore(),
                competencies(attempt),
                answers(attempt, simulation),
                latestHumanDecision(auditTrail)
        );
    }

    @Transactional
    public void registerDecision(String attemptId, RegisterResultDecisionRequest request) {
        candidateDispositionService.register(attemptId, new RegisterDispositionRequest(
                request.decision(),
                request.note()
        ));
    }

    private Specification<CandidateAttemptEntity> buildSpec(
            String tenantId,
            String search,
            String simulationId,
            AttemptStatus status,
            String integrationProvider,
            Instant periodStart,
            Instant periodEnd
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenantId"), tenantId));
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("candidateName")), pattern),
                        cb.like(cb.lower(root.get("candidateEmail")), pattern)
                ));
            }
            if (simulationId != null && !simulationId.isBlank()) {
                predicates.add(cb.equal(root.get("simulationId"), simulationId.trim()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (integrationProvider != null && !integrationProvider.isBlank()) {
                String provider = integrationProvider.trim().toUpperCase(Locale.ROOT);
                if ("MANUAL".equals(provider)) {
                    predicates.add(cb.or(
                            cb.isNull(root.get("resultWebhookUrl")),
                            cb.equal(root.get("resultWebhookUrl"), "")
                    ));
                } else if ("API".equals(provider)) {
                    predicates.add(cb.and(
                            cb.isNotNull(root.get("resultWebhookUrl")),
                            cb.notLike(cb.upper(root.get("resultWebhookUrl")), "%GUPY%"),
                            cb.notLike(cb.upper(root.get("resultWebhookUrl")), "%RECRUTEI%")
                    ));
                } else {
                    predicates.add(cb.like(cb.upper(root.get("resultWebhookUrl")), "%" + provider + "%"));
                }
            }
            if (periodStart != null || periodEnd != null) {
                Expression<Instant> date = cb.coalesce(root.get("finishedAt"), root.get("startedAt"));
                date = cb.coalesce(date, root.get("createdAt"));
                if (periodStart != null) {
                    predicates.add(cb.greaterThanOrEqualTo(date, periodStart));
                }
                if (periodEnd != null) {
                    predicates.add(cb.lessThanOrEqualTo(date, periodEnd));
                }
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private ResultListItemResponse toListItem(
            CandidateAttemptEntity attempt,
            Map<String, String> simulationTitles
    ) {
        return new ResultListItemResponse(
                attempt.getId(),
                attempt.getCandidateName(),
                attempt.getCandidateEmail(),
                attempt.getSimulationId(),
                simulationTitles.getOrDefault(attempt.getSimulationId(), "AvaliaÃ§Ã£o"),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getScore(),
                highlightCompetency(attempt),
                integrationProvider(attempt)
        );
    }

    private Map<String, String> simulationTitles(String tenantId) {
        return simulationRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .collect(Collectors.toMap(SimulationEntity::getId, SimulationEntity::getName, (left, right) -> left));
    }

    private ResultsSummaryResponse summary(List<CandidateAttemptEntity> attempts) {
        long completed = attempts.stream().filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED).count();
        long inProgress = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.IN_PROGRESS || attempt.getStatus() == AttemptStatus.NOT_STARTED || attempt.getStatus() == AttemptStatus.PAUSED)
                .count();
        long expired = attempts.stream().filter(attempt -> attempt.getStatus() == AttemptStatus.EXPIRED).count();
        List<Integer> scores = attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED)
                .map(CandidateAttemptEntity::getScore)
                .filter(score -> score != null)
                .toList();
        Integer averageScore = scores.isEmpty()
                ? null
                : Math.round((float) scores.stream().mapToInt(Integer::intValue).average().orElse(0));
        return new ResultsSummaryResponse(completed, inProgress, expired, averageScore);
    }

    private String highlightCompetency(CandidateAttemptEntity attempt) {
        return attempt.getResultItems().stream()
                .max(Comparator.comparingInt(ResultItemEntity::getScore))
                .map(ResultItemEntity::getName)
                .orElse(null);
    }

    private List<ResultDetailResponse.Competency> competencies(CandidateAttemptEntity attempt) {
        return attempt.getResultItems().stream()
                .sorted(Comparator.comparing(ResultItemEntity::getName))
                .map(item -> new ResultDetailResponse.Competency(
                        item.getName(),
                        item.getScore(),
                        competencyLevel(item.getScore()),
                        competencySummary(item.getName(), item.getScore())
                ))
                .toList();
    }

    private List<ResultDetailResponse.Answer> answers(
            CandidateAttemptEntity attempt,
            PublishedSimulation simulation
    ) {
        if (simulation == null) {
            return attempt.getAnswers().stream()
                    .sorted(Comparator.comparing(AttemptAnswerEntity::getAnsweredAt))
                    .map(answer -> new ResultDetailResponse.Answer(
                            "Etapa",
                            answer.getNodeId(),
                            answer.isTimedOut() ? "Tempo esgotado" : answer.getOptionId(),
                            null
                    ))
                    .toList();
        }

        Map<String, ScenarioNode> nodes = simulation.nodes().stream()
                .collect(Collectors.toMap(ScenarioNode::id, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        return attempt.getAnswers().stream()
                .sorted(Comparator.comparing(AttemptAnswerEntity::getAnsweredAt))
                .map(answer -> answerResponse(answer, nodes.get(answer.getNodeId())))
                .toList();
    }

    private ResultDetailResponse.Answer answerResponse(AttemptAnswerEntity answer, ScenarioNode node) {
        if (node == null) {
            return new ResultDetailResponse.Answer(
                    "Etapa",
                    answer.getNodeId(),
                    answer.isTimedOut() ? "Tempo esgotado" : answer.getOptionId(),
                    null
            );
        }

        ScenarioOption option = node.options().stream()
                .filter(candidate -> candidate.id().equals(answer.getOptionId()))
                .findFirst()
                .orElse(null);
        Integer score = option == null || option.competencyScores().isEmpty()
                ? null
                : option.competencyScores().values().stream().mapToInt(Integer::intValue).sum();
        return new ResultDetailResponse.Answer(
                "SituaÃ§Ã£o " + node.turnIndex(),
                node.message(),
                answer.isTimedOut() ? "Tempo esgotado" : option == null ? answer.getOptionId() : option.text(),
                score
        );
    }

    private Optional<PublishedSimulation> resolveSimulation(CandidateAttemptEntity attempt, String tenantId) {
        if (attempt.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(attempt.getSimulationVersionId());
        }
        return simulationCatalogService.findPublishedById(tenantId, attempt.getSimulationId());
    }

    private ResultDetailResponse.HumanDecision latestHumanDecision(List<AuditEventResponse> auditTrail) {
        return auditTrail.stream()
                .filter(event -> event.eventType() == AuditEventType.HUMAN_DECISION)
                .max(Comparator.comparing(AuditEventResponse::createdAt))
                .map(this::humanDecision)
                .orElse(new ResultDetailResponse.HumanDecision(null, null, null, null));
    }

    private ResultDetailResponse.HumanDecision humanDecision(AuditEventResponse event) {
        try {
            JsonNode metadata = objectMapper.readTree(event.metadata());
            return new ResultDetailResponse.HumanDecision(
                    text(metadata, "decision"),
                    text(metadata, "decidedByUserId"),
                    event.createdAt(),
                    text(metadata, "reason")
            );
        } catch (Exception exception) {
            return new ResultDetailResponse.HumanDecision(null, null, event.createdAt(), null);
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private String integrationProvider(CandidateAttemptEntity attempt) {
        String webhook = attempt.getResultWebhookUrl();
        if (webhook == null || webhook.isBlank()) {
            return "MANUAL";
        }
        String normalized = webhook.toUpperCase(Locale.ROOT);
        if (normalized.contains("GUPY")) {
            return "GUPY";
        }
        if (normalized.contains("RECRUTEI")) {
            return "RECRUTEI";
        }
        return "API";
    }

    private String competencyLevel(int score) {
        if (score >= 80) {
            return "ALTO";
        }
        if (score >= 60) {
            return "MEDIO";
        }
        return "BAIXO";
    }

    private String competencySummary(String name, int score) {
        return switch (competencyLevel(score)) {
            case "ALTO" -> "Demonstrou forte aderÃªncia em " + name + ".";
            case "MEDIO" -> "Apresentou desempenho adequado, com espaÃ§o para aprofundar " + name + ".";
            default -> "Requer atenÃ§Ã£o e anÃ¡lise humana cuidadosa em " + name + ".";
        };
    }
}
