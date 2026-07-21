package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse.ProgressoResponse;
import br.com.iforce.praxis.candidate.dto.ParticipationMonitoringPageResponse;
import br.com.iforce.praxis.candidate.dto.ParticipationMonitoringResponse;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.CandidateAttemptMapper;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;
import br.com.iforce.praxis.journey.model.AssessmentJourneyStepStatus;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptEntity;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptStepEntity;
import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyAttemptRepository;
import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyAttemptStepRepository;
import br.com.iforce.praxis.journey.service.AssessmentJourneyInvitationService;
import br.com.iforce.praxis.journey.service.AssessmentJourneyService;
import br.com.iforce.praxis.shared.security.EmpresaSecurity;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ParticipationMonitoringQueryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Duration ACTIVE_WINDOW = Duration.ofMinutes(5);

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AssessmentJourneyAttemptRepository journeyAttemptRepository;
    private final AssessmentJourneyAttemptStepRepository journeyAttemptStepRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final CandidateAttemptMapper candidateAttemptMapper;
    private final CandidateAttemptService candidateAttemptService;
    private final AssessmentJourneyService journeyService;
    private final AssessmentJourneyInvitationService journeyInvitationService;

    public ParticipationMonitoringQueryService(
            CandidateAttemptRepository candidateAttemptRepository,
            AssessmentJourneyAttemptRepository journeyAttemptRepository,
            AssessmentJourneyAttemptStepRepository journeyAttemptStepRepository,
            SimulationCatalogService simulationCatalogService,
            CandidateAttemptMapper candidateAttemptMapper,
            CandidateAttemptService candidateAttemptService,
            AssessmentJourneyService journeyService,
            AssessmentJourneyInvitationService journeyInvitationService
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.journeyAttemptRepository = journeyAttemptRepository;
        this.journeyAttemptStepRepository = journeyAttemptStepRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.candidateAttemptMapper = candidateAttemptMapper;
        this.candidateAttemptService = candidateAttemptService;
        this.journeyService = journeyService;
        this.journeyInvitationService = journeyInvitationService;
    }

    @Transactional(readOnly = true)
    public ParticipationMonitoringPageResponse search(
            int page,
            int size,
            String simulationId,
            String candidate
    ) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        int offset = Math.multiplyExact(normalizedPage, normalizedSize);
        int fetchSize = Math.max(1, Math.addExact(offset, normalizedSize));
        String normalizedSimulation = normalize(simulationId);
        String normalizedCandidateValue = normalize(candidate);
        String normalizedCandidate = normalizedCandidateValue == null
                ? null
                : normalizedCandidateValue.toLowerCase(Locale.ROOT);
        Set<String> journeyChildAttemptIds = journeyAttemptStepRepository
                .findCandidateAttemptIdsByEmpresaId(empresaId);

        Page<CandidateAttemptEntity> individualPage = candidateAttemptRepository.findAll(
                individualSpecification(
                        empresaId,
                        normalizedSimulation,
                        normalizedCandidate,
                        journeyChildAttemptIds
                ),
                PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Page<AssessmentJourneyAttemptEntity> journeyPage = journeyAttemptRepository.findAll(
                journeySpecification(empresaId, normalizedSimulation, normalizedCandidate),
                PageRequest.of(0, fetchSize, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        Instant now = Instant.now();
        Map<String, String> journeyNames = new HashMap<>();
        List<ParticipationMonitoringResponse> merged = new ArrayList<>(
                individualPage.getNumberOfElements() + journeyPage.getNumberOfElements()
        );
        individualPage.getContent().stream()
                .map(entity -> toIndividualResponse(entity, now))
                .forEach(merged::add);
        journeyPage.getContent().stream()
                .map(entity -> toJourneyResponse(entity, now, journeyNames))
                .forEach(merged::add);
        merged.sort(Comparator.comparing(ParticipationMonitoringResponse::createdAt).reversed());

        int fromIndex = Math.min(offset, merged.size());
        int toIndex = Math.min(fromIndex + normalizedSize, merged.size());
        List<ParticipationMonitoringResponse> items = List.copyOf(merged.subList(fromIndex, toIndex));
        long totalElements = individualPage.getTotalElements() + journeyPage.getTotalElements();
        int totalPages = totalElements == 0
                ? 0
                : (int) Math.ceil(totalElements / (double) normalizedSize);

        return new ParticipationMonitoringPageResponse(
                items,
                normalizedPage,
                normalizedSize,
                totalElements,
                totalPages
        );
    }

    private Specification<CandidateAttemptEntity> individualSpecification(
            String empresaId,
            String simulationId,
            String candidate,
            Set<String> journeyChildAttemptIds
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("empresaId"), empresaId));
            if (simulationId != null) {
                predicates.add(builder.equal(root.get("simulationId"), simulationId));
            }
            if (candidate != null) {
                String pattern = "%" + candidate + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.<String>get("candidateName")), pattern),
                        builder.like(builder.lower(root.<String>get("candidateEmail")), pattern)
                ));
            }
            if (!journeyChildAttemptIds.isEmpty()) {
                predicates.add(builder.not(root.get("id").in(journeyChildAttemptIds)));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Specification<AssessmentJourneyAttemptEntity> journeySpecification(
            String empresaId,
            String simulationId,
            String candidate
    ) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("empresaId"), empresaId));
            if (candidate != null) {
                String pattern = "%" + candidate + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.<String>get("candidateName")), pattern),
                        builder.like(builder.lower(root.<String>get("candidateEmail")), pattern)
                ));
            }
            if (simulationId != null) {
                predicates.add(builder.equal(root.join("steps").get("simulationId"), simulationId));
                query.distinct(true);
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private ParticipationMonitoringResponse toIndividualResponse(
            CandidateAttemptEntity entity,
            Instant now
    ) {
        PublishedSimulation simulation = findSimulation(entity);
        CandidateAttempt attempt = candidateAttemptMapper.toDomain(entity);
        ScenarioNode currentNode = findCurrentNode(attempt, simulation);
        ProgressoResponse progress = progressFor(attempt, simulation, currentNode);
        Instant lastSignalAt = lastSignalAt(entity);
        Instant startedAt = entity.getStartedAt() == null ? entity.getCreatedAt() : entity.getStartedAt();
        Instant elapsedUntil = entity.getFinishedAt() == null ? now : entity.getFinishedAt();
        Instant expiresAt = entity.getCandidateTokenExpiresAt();
        boolean expired = expiresAt != null && !expiresAt.isAfter(now);
        boolean closed = entity.getStatus() == AttemptStatus.COMPLETED
                || entity.getStatus() == AttemptStatus.ABANDONED;
        String status = expired && !closed ? "expired" : entity.getStatus().getDescricao();

        return new ParticipationMonitoringResponse(
                entity.getId(),
                "individual",
                entity.getCandidateName(),
                entity.getCandidateEmail(),
                entity.getSimulationId(),
                simulation.name(),
                simulation.versionNumber(),
                null,
                null,
                null,
                status,
                progress.passoAtual(),
                progress.passosEstimados(),
                progress.percentual(),
                Math.max(0, Duration.between(startedAt, elapsedUntil).toSeconds()),
                lastSignalAt,
                entity.getStatus() == AttemptStatus.IN_PROGRESS
                        && !lastSignalAt.isBefore(now.minus(ACTIVE_WINDOW)),
                candidateAttemptService.candidatePageUrlFor(entity.getEmpresaId(), entity.getId()),
                expiresAt,
                linkStatus(expiresAt, closed ? entity.getStatus() == AttemptStatus.ABANDONED : false, now),
                remainingDays(expiresAt, now),
                !closed && !expired,
                !closed,
                false,
                entity.getStatus() == AttemptStatus.COMPLETED ? entity.getId() : null,
                entity.getCreatedAt()
        );
    }

    private ParticipationMonitoringResponse toJourneyResponse(
            AssessmentJourneyAttemptEntity entity,
            Instant now,
            Map<String, String> journeyNames
    ) {
        int totalSteps = entity.getSteps().size();
        long completedSteps = entity.getSteps().stream()
                .filter(step -> step.getStatus() == AssessmentJourneyStepStatus.COMPLETED)
                .count();
        int progressPercent = totalSteps == 0
                ? 0
                : Math.min(100, Math.round((completedSteps * 100f) / totalSteps));
        int currentTurn = entity.getStatus() == AssessmentJourneyAttemptStatus.COMPLETED
                ? totalSteps
                : Math.min(totalSteps, (int) completedSteps + 1);
        Instant lastSignalAt = journeyLastSignalAt(entity);
        Instant startedAt = entity.getStartedAt() == null ? entity.getCreatedAt() : entity.getStartedAt();
        Instant elapsedUntil = entity.getCompletedAt() == null ? now : entity.getCompletedAt();
        boolean canceled = entity.getStatus() == AssessmentJourneyAttemptStatus.ABANDONED
                || entity.getCanceledAt() != null;
        boolean completed = entity.getStatus() == AssessmentJourneyAttemptStatus.COMPLETED;
        boolean expired = entity.getStatus() == AssessmentJourneyAttemptStatus.EXPIRED
                || (entity.getExpiresAt() != null && !entity.getExpiresAt().isAfter(now));
        String status = canceled
                ? "abandoned"
                : expired && !completed
                    ? "expired"
                    : entity.getStatus().getDescricao();
        String journeyName = journeyNames.computeIfAbsent(
                entity.getJourneyId(),
                journeyId -> journeyService.findJourneyForEmpresa(entity.getEmpresaId(), journeyId)
                        .map(journey -> journey.getName())
                        .orElse(journeyId)
        );

        return new ParticipationMonitoringResponse(
                entity.getId(),
                "journey",
                entity.getCandidateName(),
                entity.getCandidateEmail(),
                null,
                null,
                null,
                entity.getJourneyId(),
                journeyName,
                entity.getSequenceKey(),
                status,
                currentTurn,
                totalSteps,
                progressPercent,
                Math.max(0, Duration.between(startedAt, elapsedUntil).toSeconds()),
                lastSignalAt,
                entity.getStatus() == AssessmentJourneyAttemptStatus.IN_PROGRESS
                        && !lastSignalAt.isBefore(now.minus(ACTIVE_WINDOW)),
                journeyInvitationService.journeyAttemptUrl(entity.getId()),
                entity.getExpiresAt(),
                linkStatus(entity.getExpiresAt(), canceled, now),
                remainingDays(entity.getExpiresAt(), now),
                !completed && !canceled && !expired,
                !completed && !canceled,
                !completed && !canceled,
                null,
                entity.getCreatedAt()
        );
    }

    private PublishedSimulation findSimulation(CandidateAttemptEntity entity) {
        if (entity.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(entity.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "A versão associada à tentativa não foi encontrada."
                    ));
        }
        return simulationCatalogService.findPublishedById(entity.getEmpresaId(), entity.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "A avaliação associada à tentativa não foi encontrada."
                ));
    }

    private ScenarioNode findCurrentNode(CandidateAttempt attempt, PublishedSimulation simulation) {
        String nodeId = simulation.rootNodeId();
        Set<String> visited = new HashSet<>();
        while (nodeId != null && visited.add(nodeId)) {
            ScenarioNode node = simulationCatalogService.findNode(simulation, nodeId).orElse(null);
            if (node == null || node.isFinal()) {
                return null;
            }
            AttemptAnswer answer = attempt.answersByNodeId().get(node.id());
            if (answer == null) {
                return node;
            }
            if (answer.timedOut() || answer.optionId() == null) {
                nodeId = node.timeoutNextNodeId();
                continue;
            }
            ScenarioOption selectedOption = node.options().stream()
                    .filter(option -> option.id().equals(answer.optionId()))
                    .findFirst()
                    .orElse(null);
            nodeId = selectedOption == null ? null : selectedOption.nextNodeId();
        }
        return null;
    }

    private ProgressoResponse progressFor(
            CandidateAttempt attempt,
            PublishedSimulation simulation,
            ScenarioNode currentNode
    ) {
        int answeredSteps = attempt.answersByNodeId().size();
        int remainingSteps = currentNode == null
                ? 0
                : maxRemainingDepth(simulation, currentNode.id(), new HashSet<>());
        int estimatedSteps = Math.max(1, answeredSteps + remainingSteps);
        int currentStep = currentNode == null
                ? estimatedSteps
                : Math.min(estimatedSteps, answeredSteps + 1);
        int percent = currentNode == null
                ? 100
                : Math.min(100, Math.max(1, Math.round((currentStep * 100f) / estimatedSteps)));
        return new ProgressoResponse(currentStep, estimatedSteps, percent);
    }

    private int maxRemainingDepth(
            PublishedSimulation simulation,
            String nodeId,
            Set<String> visited
    ) {
        if (nodeId == null || !visited.add(nodeId)) {
            return 0;
        }
        ScenarioNode node = simulationCatalogService.findNode(simulation, nodeId).orElse(null);
        if (node == null || node.isFinal()) {
            return 0;
        }
        int optionDepth = node.options().stream()
                .map(ScenarioOption::nextNodeId)
                .mapToInt(next -> maxRemainingDepth(simulation, next, new HashSet<>(visited)))
                .max()
                .orElse(0);
        int timeoutDepth = maxRemainingDepth(
                simulation,
                node.timeoutNextNodeId(),
                new HashSet<>(visited)
        );
        return 1 + Math.max(optionDepth, timeoutDepth);
    }

    private Instant lastSignalAt(CandidateAttemptEntity entity) {
        Instant base = entity.getStartedAt() == null ? entity.getCreatedAt() : entity.getStartedAt();
        return entity.getAnswers().stream()
                .map(answer -> answer.getAnsweredAt() == null ? base : answer.getAnsweredAt())
                .max(Comparator.naturalOrder())
                .orElse(base);
    }

    private Instant journeyLastSignalAt(AssessmentJourneyAttemptEntity entity) {
        Instant base = entity.getStartedAt() == null ? entity.getCreatedAt() : entity.getStartedAt();
        return entity.getSteps().stream()
                .flatMap(step -> List.of(step.getStartedAt(), step.getCompletedAt()).stream())
                .filter(value -> value != null)
                .max(Comparator.naturalOrder())
                .orElse(base);
    }

    private String linkStatus(Instant expiresAt, boolean canceled, Instant now) {
        if (canceled) {
            return "canceled";
        }
        long days = remainingDays(expiresAt, now);
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            return "expired";
        }
        return days <= 2 ? "expiringSoon" : "active";
    }

    private long remainingDays(Instant expiresAt, Instant now) {
        if (expiresAt == null || !expiresAt.isAfter(now)) {
            return 0;
        }
        long seconds = Duration.between(now, expiresAt).toSeconds();
        return Math.max(1, (seconds + 86_399L) / 86_400L);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
