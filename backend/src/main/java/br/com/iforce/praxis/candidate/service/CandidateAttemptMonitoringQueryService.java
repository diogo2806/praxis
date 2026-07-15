package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.candidate.dto.CandidateAttemptMonitoringPageResponse;
import br.com.iforce.praxis.candidate.dto.CandidateAttemptMonitoringResponse;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse.ProgressoResponse;
import br.com.iforce.praxis.gupy.model.AttemptAnswer;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ScenarioNode;
import br.com.iforce.praxis.gupy.model.ScenarioOption;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.CandidateAttemptMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CandidateAttemptMonitoringQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final CandidateAttemptMapper candidateAttemptMapper;

    public CandidateAttemptMonitoringQueryService(
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationCatalogService simulationCatalogService,
            CandidateAttemptMapper candidateAttemptMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.candidateAttemptMapper = candidateAttemptMapper;
    }

    @Transactional(readOnly = true)
    public CandidateAttemptMonitoringPageResponse search(
            int page,
            int size,
            String status,
            String simulationId,
            String candidate
    ) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        AttemptStatus parsedStatus = parseStatus(status);
        String normalizedSimulation = normalize(simulationId);
        String normalizedCandidate = normalize(candidate).toLowerCase(Locale.ROOT);

        Specification<CandidateAttemptEntity> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("empresaId"), empresaId));
            if (parsedStatus != null) {
                predicates.add(builder.equal(root.get("status"), parsedStatus));
            }
            if (normalizedSimulation != null) {
                predicates.add(builder.equal(root.get("simulationId"), normalizedSimulation));
            }
            if (normalizedCandidate != null) {
                String pattern = "%" + normalizedCandidate + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("candidateName")), pattern),
                        builder.like(builder.lower(root.get("candidateEmail")), pattern)
                ));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };

        Page<CandidateAttemptEntity> result = candidateAttemptRepository.findAll(
                specification,
                PageRequest.of(
                        normalizedPage,
                        normalizedSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );
        Instant now = Instant.now();
        List<CandidateAttemptMonitoringResponse> items = result.getContent().stream()
                .map(entity -> toResponse(entity, now))
                .toList();

        return new CandidateAttemptMonitoringPageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private CandidateAttemptMonitoringResponse toResponse(CandidateAttemptEntity entity, Instant now) {
        PublishedSimulation simulation = simulationCatalogService.findByVersionId(entity.getSimulationVersionId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "A versão associada à tentativa não foi encontrada."
                ));
        CandidateAttempt attempt = candidateAttemptMapper.toDomain(entity);
        ScenarioNode currentNode = findCurrentNode(attempt, simulation);
        ProgressoResponse progress = progressFor(attempt, simulation, currentNode);
        Instant lastSignalAt = lastSignalAt(entity);
        Instant startedAt = entity.getStartedAt() == null ? entity.getCreatedAt() : entity.getStartedAt();
        Instant elapsedUntil = entity.getFinishedAt() == null ? now : entity.getFinishedAt();

        return new CandidateAttemptMonitoringResponse(
                entity.getId(),
                entity.getCandidateName(),
                entity.getCandidateEmail(),
                entity.getSimulationId(),
                simulation.name(),
                simulation.versionNumber(),
                entity.getStatus(),
                progress.passoAtual(),
                progress.passosEstimados(),
                progress.percentual(),
                Math.max(0, Duration.between(startedAt, elapsedUntil).toSeconds()),
                lastSignalAt,
                entity.getStatus() == AttemptStatus.IN_PROGRESS
                        && !lastSignalAt.isBefore(now.minus(Duration.ofMinutes(5)))
        );
    }

    private ScenarioNode findCurrentNode(CandidateAttempt attempt, PublishedSimulation simulation) {
        String nodeId = simulation.rootNodeId();
        Set<String> visited = new HashSet<>();
        while (nodeId != null && visited.add(nodeId)) {
            ScenarioNode node = simulationCatalogService.findNode(simulation, nodeId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "A avaliação possui uma etapa inexistente."
                    ));
            if (node.isFinal()) {
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
            nodeId = node.options().stream()
                    .filter(option -> option.id().equals(answer.optionId()))
                    .map(ScenarioOption::nextNodeId)
                    .findFirst()
                    .orElse(null);
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

    private AttemptStatus parseStatus(String status) {
        String normalized = normalize(status);
        if (normalized == null) {
            return null;
        }
        try {
            return AttemptStatus.fromString(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de tentativa inválido.");
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
