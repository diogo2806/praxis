package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;
import br.com.iforce.praxis.journey.dto.CreateJourneyAttemptRequest;
import br.com.iforce.praxis.journey.dto.JourneyAttemptStepResponse;
import br.com.iforce.praxis.journey.dto.JourneyConsolidatedResultResponse;
import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;
import br.com.iforce.praxis.journey.model.AssessmentJourneyStatus;
import br.com.iforce.praxis.journey.model.AssessmentJourneyStepStatus;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptEntity;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptStepEntity;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyEntity;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyStepEntity;
import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyAttemptRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cérebro da execução das Jornadas de Avaliação pelo candidato.
 *
 * <p>Orquestra a tentativa da jornada: cria a tentativa para um candidato,
 * controla o progresso geral, libera os testes na ordem definida e reaproveita
 * o mecanismo de tentativa individual ({@code CandidateAttempt}) de cada teste,
 * sem criar uma nova engine de prova. A jornada consolida — não substitui — os
 * resultados individuais. Tudo fica registrado na trilha append-only.</p>
 */
@Service
public class AssessmentJourneyAttemptService {

    private final AssessmentJourneyAttemptRepository attemptRepository;
    private final AssessmentJourneyService journeyService;
    private final CandidateAttemptService candidateAttemptService;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final CurrentTenantService currentTenantService;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;

    public AssessmentJourneyAttemptService(
            AssessmentJourneyAttemptRepository attemptRepository,
            AssessmentJourneyService journeyService,
            CandidateAttemptService candidateAttemptService,
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationCatalogService simulationCatalogService,
            CurrentTenantService currentTenantService,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata
    ) {
        this.attemptRepository = attemptRepository;
        this.journeyService = journeyService;
        this.candidateAttemptService = candidateAttemptService;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.currentTenantService = currentTenantService;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
    }

    /**
     * Cria a tentativa de uma jornada publicada para um candidato.
     *
     * <p>Materializa, na ordem definida, as etapas da sequência escolhida (ou da
     * primeira sequência, quando não informada), cada uma pronta para gerar a
     * sua tentativa individual de teste.</p>
     *
     * @param request jornada, dados do candidato e sequência escolhida
     * @return o progresso inicial da tentativa
     */
    @Transactional
    public AssessmentJourneyAttemptResponse createAttempt(CreateJourneyAttemptRequest request) {
        String tenantId = currentTenantService.requiredTenantId();
        AssessmentJourneyEntity journey = journeyService.findJourneyForTenant(tenantId, request.journeyId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jornada de avaliação não encontrada."));

        if (journey.getStatus() != AssessmentJourneyStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Só é possível gerar tentativas de uma jornada publicada."
            );
        }

        String sequenceKey = resolveSequenceKey(journey, request.sequenceKey());
        List<AssessmentJourneyStepEntity> sequenceSteps = orderedSequenceSteps(journey, sequenceKey);
        if (sequenceSteps.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A sequência escolhida não possui testes.");
        }

        Instant now = Instant.now();
        AssessmentJourneyAttemptEntity attempt = new AssessmentJourneyAttemptEntity();
        attempt.setId("jatt_" + UUID.randomUUID().toString().replace("-", ""));
        attempt.setTenantId(tenantId);
        attempt.setJourneyId(journey.getId());
        attempt.setCandidateName(request.candidateName().trim());
        attempt.setCandidateEmail(request.candidateEmail().trim());
        attempt.setSequenceKey(sequenceKey);
        attempt.setStatus(AssessmentJourneyAttemptStatus.CREATED);
        attempt.setCreatedAt(now);

        for (AssessmentJourneyStepEntity sequenceStep : sequenceSteps) {
            AssessmentJourneyAttemptStepEntity attemptStep = new AssessmentJourneyAttemptStepEntity();
            attemptStep.setTenantId(tenantId);
            attemptStep.setJourneyAttempt(attempt);
            attemptStep.setJourneyStepId(sequenceStep.getId());
            attemptStep.setSimulationId(sequenceStep.getSimulationId());
            attemptStep.setSimulationVersionNumber(sequenceStep.getSimulationVersionNumber());
            attemptStep.setOrderIndex(sequenceStep.getOrderIndex());
            attemptStep.setRequired(sequenceStep.isRequired());
            attemptStep.setStatus(AssessmentJourneyStepStatus.PENDING);
            attemptStep.setCreatedAt(now);
            attempt.getSteps().add(attemptStep);
        }

        AssessmentJourneyAttemptEntity saved = attemptRepository.save(attempt);
        auditEventService.appendAssessmentJourneyAttemptEvent(
                tenantId,
                saved.getId(),
                AuditEventType.ASSESSMENT_JOURNEY_ATTEMPT_CREATED,
                "Tentativa da jornada criada para o candidato.",
                auditMetadata.of(
                        "journeyId", journey.getId(),
                        "sequenceKey", sequenceKey,
                        "stepCount", sequenceSteps.size()
                )
        );
        return toResponse(saved, journey, Map.of());
    }

    /**
     * Consulta o progresso da tentativa da jornada.
     *
     * @param attemptId identificador da tentativa da jornada
     * @return o progresso atual
     */
    @Transactional(readOnly = true)
    public AssessmentJourneyAttemptResponse getAttempt(String attemptId) {
        AssessmentJourneyAttemptEntity attempt = findAttempt(attemptId);
        return toResponse(attempt, loadJourney(attempt), Map.of());
    }

    /**
     * Lista as tentativas de candidatos de uma jornada, da mais recente à mais antiga.
     *
     * @param journeyId identificador da jornada
     * @return o progresso de cada tentativa
     */
    @Transactional(readOnly = true)
    public List<AssessmentJourneyAttemptResponse> listAttemptsByJourney(String journeyId) {
        String tenantId = currentTenantService.requiredTenantId();
        AssessmentJourneyEntity journey = journeyService.findJourneyForTenant(tenantId, journeyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jornada de avaliação não encontrada."));
        return attemptRepository.findByTenantIdAndJourneyIdOrderByCreatedAtDesc(tenantId, journeyId)
                .stream()
                .map(attempt -> toResponse(attempt, journey, Map.of()))
                .toList();
    }

    /**
     * Inicia a jornada (marca a tentativa como em andamento).
     *
     * @param attemptId identificador da tentativa da jornada
     * @return o progresso atualizado
     */
    @Transactional
    public AssessmentJourneyAttemptResponse startAttempt(String attemptId) {
        AssessmentJourneyAttemptEntity attempt = findAttempt(attemptId);
        assertNotTerminal(attempt);

        if (attempt.getStatus() == AssessmentJourneyAttemptStatus.CREATED) {
            attempt.setStatus(AssessmentJourneyAttemptStatus.IN_PROGRESS);
            attempt.setStartedAt(Instant.now());
            attemptRepository.save(attempt);
            auditEventService.appendAssessmentJourneyAttemptEvent(
                    attempt.getTenantId(),
                    attempt.getId(),
                    AuditEventType.ASSESSMENT_JOURNEY_ATTEMPT_STARTED,
                    "Tentativa da jornada iniciada.",
                    auditMetadata.of("journeyId", attempt.getJourneyId())
            );
        }
        return toResponse(attempt, loadJourney(attempt), Map.of());
    }

    /**
     * Inicia uma etapa/teste da jornada, gerando a tentativa individual do teste.
     *
     * <p>Respeita a ordem: o candidato só avança quando as etapas obrigatórias
     * anteriores da sequência já tiverem sido concluídas.</p>
     *
     * @param attemptId identificador da tentativa da jornada
     * @param stepId identificador da etapa da tentativa
     * @return o progresso atualizado, com o link do candidato para o teste iniciado
     */
    @Transactional
    public AssessmentJourneyAttemptResponse startStep(String attemptId, Long stepId) {
        AssessmentJourneyAttemptEntity attempt = findAttempt(attemptId);
        assertNotTerminal(attempt);
        AssessmentJourneyAttemptStepEntity step = findStep(attempt, stepId);
        assertPreviousRequiredStepsCompleted(attempt, step);

        Instant now = Instant.now();
        if (attempt.getStatus() == AssessmentJourneyAttemptStatus.CREATED) {
            attempt.setStatus(AssessmentJourneyAttemptStatus.IN_PROGRESS);
            attempt.setStartedAt(now);
            auditEventService.appendAssessmentJourneyAttemptEvent(
                    attempt.getTenantId(),
                    attempt.getId(),
                    AuditEventType.ASSESSMENT_JOURNEY_ATTEMPT_STARTED,
                    "Tentativa da jornada iniciada.",
                    auditMetadata.of("journeyId", attempt.getJourneyId())
            );
        }

        if (step.getStatus() == AssessmentJourneyStepStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta etapa da jornada já foi concluída.");
        }

        // Reaproveita o mecanismo de tentativa individual (idempotente por candidato + simulação).
        CreateCandidateLinkResponse link = candidateAttemptService.createCompanyLink(new CreateCandidateLinkRequest(
                step.getSimulationId(),
                attempt.getCandidateName(),
                attempt.getCandidateEmail(),
                null
        ));

        boolean firstStart = step.getCandidateAttemptId() == null;
        step.setCandidateAttemptId(link.attemptId());
        if (step.getStatus() == AssessmentJourneyStepStatus.PENDING) {
            step.setStatus(AssessmentJourneyStepStatus.IN_PROGRESS);
        }
        if (step.getStartedAt() == null) {
            step.setStartedAt(now);
        }
        attemptRepository.save(attempt);

        if (firstStart) {
            auditEventService.appendAssessmentJourneyAttemptEvent(
                    attempt.getTenantId(),
                    attempt.getId(),
                    AuditEventType.ASSESSMENT_JOURNEY_STEP_STARTED,
                    "Etapa da jornada iniciada.",
                    auditMetadata.of(
                            "journeyStepId", step.getJourneyStepId(),
                            "simulationId", step.getSimulationId(),
                            "candidateAttemptId", link.attemptId()
                    )
            );
        }

        Map<String, String> urlByAttemptId = new LinkedHashMap<>();
        urlByAttemptId.put(link.attemptId(), link.candidateUrl());
        return toResponse(attempt, loadJourney(attempt), urlByAttemptId);
    }

    /**
     * Marca uma etapa da jornada como concluída após a conclusão da tentativa
     * individual ({@code CandidateAttempt}).
     *
     * <p>Quando todas as etapas obrigatórias da sequência estiverem concluídas,
     * a tentativa da jornada é marcada como concluída.</p>
     *
     * @param attemptId identificador da tentativa da jornada
     * @param stepId identificador da etapa da tentativa
     * @return o progresso atualizado
     */
    @Transactional
    public AssessmentJourneyAttemptResponse completeStep(String attemptId, Long stepId) {
        AssessmentJourneyAttemptEntity attempt = findAttempt(attemptId);
        assertNotTerminal(attempt);
        AssessmentJourneyAttemptStepEntity step = findStep(attempt, stepId);

        if (step.getStatus() == AssessmentJourneyStepStatus.COMPLETED) {
            return toResponse(attempt, loadJourney(attempt), Map.of());
        }
        if (step.getCandidateAttemptId() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Inicie a etapa antes de concluí-la.");
        }

        CandidateAttemptEntity candidateAttempt = candidateAttemptRepository
                .findByTenantIdAndId(attempt.getTenantId(), step.getCandidateAttemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa individual do teste não encontrada."));
        if (candidateAttempt.getStatus() != AttemptStatus.COMPLETED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O teste ainda não foi concluído pelo candidato."
            );
        }

        Instant now = Instant.now();
        step.setStatus(AssessmentJourneyStepStatus.COMPLETED);
        step.setCompletedAt(now);
        if (attempt.getStatus() == AssessmentJourneyAttemptStatus.CREATED) {
            attempt.setStatus(AssessmentJourneyAttemptStatus.IN_PROGRESS);
            attempt.setStartedAt(attempt.getStartedAt() == null ? now : attempt.getStartedAt());
        }
        auditEventService.appendAssessmentJourneyAttemptEvent(
                attempt.getTenantId(),
                attempt.getId(),
                AuditEventType.ASSESSMENT_JOURNEY_STEP_COMPLETED,
                "Etapa da jornada concluída.",
                auditMetadata.of(
                        "journeyStepId", step.getJourneyStepId(),
                        "simulationId", step.getSimulationId(),
                        "candidateAttemptId", step.getCandidateAttemptId()
                )
        );

        if (allRequiredStepsCompleted(attempt)) {
            attempt.setStatus(AssessmentJourneyAttemptStatus.COMPLETED);
            attempt.setCompletedAt(now);
            auditEventService.appendAssessmentJourneyAttemptEvent(
                    attempt.getTenantId(),
                    attempt.getId(),
                    AuditEventType.ASSESSMENT_JOURNEY_ATTEMPT_COMPLETED,
                    "Tentativa da jornada concluída.",
                    auditMetadata.of("journeyId", attempt.getJourneyId())
            );
        }
        attemptRepository.save(attempt);
        return toResponse(attempt, loadJourney(attempt), Map.of());
    }

    /**
     * Monta o resultado consolidado do candidato na jornada.
     *
     * <p>Conforme o MVP, os resultados são exibidos separados por teste (status,
     * pontuação e competências de cada um); a nota geral ponderada fica para
     * uma etapa posterior.</p>
     *
     * @param attemptId identificador da tentativa da jornada
     * @return o resultado consolidado
     */
    @Transactional(readOnly = true)
    public JourneyConsolidatedResultResponse getConsolidatedResult(String attemptId) {
        AssessmentJourneyAttemptEntity attempt = findAttempt(attemptId);
        AssessmentJourneyEntity journey = loadJourney(attempt);
        Map<String, String> nameCache = new LinkedHashMap<>();

        List<JourneyConsolidatedResultResponse.TestResult> tests = orderedSteps(attempt).stream()
                .map(step -> toTestResult(attempt.getTenantId(), step, nameCache))
                .toList();

        return new JourneyConsolidatedResultResponse(
                attempt.getId(),
                attempt.getJourneyId(),
                journey == null ? attempt.getJourneyId() : journey.getName(),
                attempt.getCandidateName(),
                attempt.getCandidateEmail(),
                attempt.getSequenceKey(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                tests
        );
    }

    // ----- helpers -----

    private JourneyConsolidatedResultResponse.TestResult toTestResult(
            String tenantId,
            AssessmentJourneyAttemptStepEntity step,
            Map<String, String> nameCache
    ) {
        String simulationName = resolveSimulationName(tenantId, step.getSimulationId(), nameCache);
        AttemptStatus attemptStatus = null;
        Integer score = null;
        List<JourneyConsolidatedResultResponse.CompetencyResult> competencies = List.of();

        if (step.getCandidateAttemptId() != null) {
            CandidateAttemptEntity candidateAttempt = candidateAttemptRepository
                    .findByTenantIdAndId(tenantId, step.getCandidateAttemptId())
                    .orElse(null);
            if (candidateAttempt != null) {
                attemptStatus = candidateAttempt.getStatus();
                if (candidateAttempt.getStatus() == AttemptStatus.COMPLETED) {
                    score = candidateAttempt.getScore();
                }
                competencies = candidateAttempt.getResultItems().stream()
                        .sorted(Comparator.comparing(ResultItemEntity::getName))
                        .map(item -> new JourneyConsolidatedResultResponse.CompetencyResult(item.getName(), item.getScore()))
                        .toList();
            }
        }

        return new JourneyConsolidatedResultResponse.TestResult(
                step.getSimulationId(),
                simulationName,
                step.getSimulationVersionNumber(),
                step.isRequired(),
                step.getStatus(),
                step.getCandidateAttemptId(),
                attemptStatus,
                score,
                competencies
        );
    }

    private void assertPreviousRequiredStepsCompleted(
            AssessmentJourneyAttemptEntity attempt,
            AssessmentJourneyAttemptStepEntity target
    ) {
        for (AssessmentJourneyAttemptStepEntity step : orderedSteps(attempt)) {
            if (step.getOrderIndex() >= target.getOrderIndex()) {
                break;
            }
            if (step.isRequired() && step.getStatus() != AssessmentJourneyStepStatus.COMPLETED) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Conclua o teste obrigatório anterior antes de iniciar este."
                );
            }
        }
    }

    private boolean allRequiredStepsCompleted(AssessmentJourneyAttemptEntity attempt) {
        return attempt.getSteps().stream()
                .filter(AssessmentJourneyAttemptStepEntity::isRequired)
                .allMatch(step -> step.getStatus() == AssessmentJourneyStepStatus.COMPLETED);
    }

    private void assertNotTerminal(AssessmentJourneyAttemptEntity attempt) {
        AssessmentJourneyAttemptStatus status = attempt.getStatus();
        if (status == AssessmentJourneyAttemptStatus.COMPLETED
                || status == AssessmentJourneyAttemptStatus.EXPIRED
                || status == AssessmentJourneyAttemptStatus.ABANDONED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta tentativa da jornada já foi encerrada.");
        }
    }

    private AssessmentJourneyAttemptEntity findAttempt(String attemptId) {
        String tenantId = currentTenantService.requiredTenantId();
        return attemptRepository.findByTenantIdAndId(tenantId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa da jornada não encontrada."));
    }

    private AssessmentJourneyAttemptStepEntity findStep(AssessmentJourneyAttemptEntity attempt, Long stepId) {
        return attempt.getSteps().stream()
                .filter(step -> step.getId() != null && step.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa da tentativa não encontrada."));
    }

    private AssessmentJourneyEntity loadJourney(AssessmentJourneyAttemptEntity attempt) {
        return journeyService.findJourneyForTenant(attempt.getTenantId(), attempt.getJourneyId()).orElse(null);
    }

    private List<AssessmentJourneyAttemptStepEntity> orderedSteps(AssessmentJourneyAttemptEntity attempt) {
        return attempt.getSteps().stream()
                .sorted(Comparator.comparingInt(AssessmentJourneyAttemptStepEntity::getOrderIndex))
                .toList();
    }

    private List<AssessmentJourneyStepEntity> orderedSequenceSteps(AssessmentJourneyEntity journey, String sequenceKey) {
        return journey.getSteps().stream()
                .filter(step -> step.getSequenceKey().equals(sequenceKey))
                .sorted(Comparator.comparingInt(AssessmentJourneyStepEntity::getOrderIndex))
                .toList();
    }

    private String resolveSequenceKey(AssessmentJourneyEntity journey, String requestedSequenceKey) {
        List<String> sequenceKeys = journey.getSteps().stream()
                .map(AssessmentJourneyStepEntity::getSequenceKey)
                .distinct()
                .sorted()
                .toList();
        if (sequenceKeys.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A jornada não possui sequências configuradas.");
        }
        if (requestedSequenceKey == null || requestedSequenceKey.isBlank()) {
            return sequenceKeys.get(0);
        }
        String normalized = requestedSequenceKey.trim();
        if (!sequenceKeys.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A sequência informada não existe nesta jornada.");
        }
        return normalized;
    }

    private String resolveSimulationName(String tenantId, String simulationId, Map<String, String> nameCache) {
        return nameCache.computeIfAbsent(simulationId, id ->
                simulationCatalogService.findPublishedById(tenantId, id)
                        .map(PublishedSimulation::name)
                        .orElse(id));
    }

    private AssessmentJourneyAttemptResponse toResponse(
            AssessmentJourneyAttemptEntity attempt,
            AssessmentJourneyEntity journey,
            Map<String, String> urlByAttemptId
    ) {
        Map<String, String> nameCache = new LinkedHashMap<>();
        List<JourneyAttemptStepResponse> steps = new ArrayList<>();
        for (AssessmentJourneyAttemptStepEntity step : orderedSteps(attempt)) {
            String candidateUrl = null;
            if (step.getCandidateAttemptId() != null) {
                candidateUrl = urlByAttemptId.get(step.getCandidateAttemptId());
                if (candidateUrl == null) {
                    candidateUrl = candidateAttemptService.candidatePageUrlFor(attempt.getTenantId(), step.getCandidateAttemptId());
                }
            }
            steps.add(new JourneyAttemptStepResponse(
                    step.getId(),
                    step.getJourneyStepId(),
                    step.getSimulationId(),
                    resolveSimulationName(attempt.getTenantId(), step.getSimulationId(), nameCache),
                    step.getOrderIndex(),
                    step.isRequired(),
                    step.getStatus(),
                    step.getCandidateAttemptId(),
                    candidateUrl,
                    step.getStartedAt(),
                    step.getCompletedAt()
            ));
        }

        return new AssessmentJourneyAttemptResponse(
                attempt.getId(),
                attempt.getJourneyId(),
                journey == null ? attempt.getJourneyId() : journey.getName(),
                attempt.getCandidateName(),
                attempt.getCandidateEmail(),
                attempt.getSequenceKey(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                attempt.getCreatedAt(),
                steps
        );
    }
}
