package br.com.iforce.praxis.journey.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.journey.dto.AddJourneyStepRequest;
import br.com.iforce.praxis.journey.dto.AssessmentJourneyDetailResponse;
import br.com.iforce.praxis.journey.dto.AssessmentJourneySummaryResponse;
import br.com.iforce.praxis.journey.dto.CreateAssessmentJourneyRequest;
import br.com.iforce.praxis.journey.dto.JourneyStepResponse;
import br.com.iforce.praxis.journey.dto.UpdateAssessmentJourneyRequest;
import br.com.iforce.praxis.journey.dto.UpdateJourneyStepRequest;
import br.com.iforce.praxis.journey.model.AssessmentJourneyStatus;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyEntity;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyStepEntity;
import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cérebro da autoria das Jornadas de Avaliação.
 *
 * <p>Orquestra a criação, edição, composição (sequências e testes), publicação
 * e arquivamento das jornadas. Não duplica a lógica de teste: apenas reaproveita
 * simulações já publicadas, agrupando-as em uma experiência maior. Toda
 * alteração relevante fica registrada na trilha de auditoria append-only.</p>
 */
@Service
public class AssessmentJourneyService {

    /** Sequência padrão do MVP quando o cliente não informa uma. */
    public static final String DEFAULT_SEQUENCE_KEY = "principal";

    private final AssessmentJourneyRepository journeyRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final CurrentTenantService currentTenantService;
    private final CurrentUserService currentUserService;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;

    public AssessmentJourneyService(
            AssessmentJourneyRepository journeyRepository,
            SimulationCatalogService simulationCatalogService,
            CurrentTenantService currentTenantService,
            CurrentUserService currentUserService,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata
    ) {
        this.journeyRepository = journeyRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.currentTenantService = currentTenantService;
        this.currentUserService = currentUserService;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
    }

    /**
     * Cria uma jornada nova em rascunho.
     *
     * @param request nome e descrição da jornada
     * @return o detalhe da jornada criada
     */
    @Transactional
    public AssessmentJourneyDetailResponse createJourney(CreateAssessmentJourneyRequest request) {
        String tenantId = currentTenantService.requiredTenantId();
        Instant now = Instant.now();

        AssessmentJourneyEntity journey = new AssessmentJourneyEntity();
        journey.setId(generateJourneyId(request.name()));
        journey.setTenantId(tenantId);
        journey.setName(request.name().trim());
        journey.setDescription(trimToNull(request.description()));
        journey.setStatus(AssessmentJourneyStatus.DRAFT);
        journey.setCreatedAt(now);
        journey.setUpdatedAt(now);

        AssessmentJourneyEntity saved = journeyRepository.save(journey);
        auditEventService.appendAssessmentJourneyEvent(
                tenantId,
                currentUserService.requiredUserId(),
                saved.getId(),
                AuditEventType.ASSESSMENT_JOURNEY_CREATED,
                "Jornada de avaliação criada em rascunho.",
                auditMetadata.of("name", saved.getName())
        );

        return toDetailResponse(saved);
    }

    /**
     * Lista as jornadas do tenant, da mais recente para a mais antiga.
     *
     * @return o resumo de cada jornada
     */
    @Transactional(readOnly = true)
    public List<AssessmentJourneySummaryResponse> listJourneys() {
        String tenantId = currentTenantService.requiredTenantId();
        return journeyRepository.findByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /**
     * Detalha uma jornada, com suas sequências e testes em ordem.
     *
     * @param journeyId identificador da jornada
     * @return o detalhe completo da jornada
     */
    @Transactional(readOnly = true)
    public AssessmentJourneyDetailResponse getJourney(String journeyId) {
        return toDetailResponse(findJourney(journeyId));
    }

    /**
     * Atualiza os dados básicos (nome/descrição) de uma jornada em rascunho.
     *
     * @param journeyId identificador da jornada
     * @param request novos dados básicos
     * @return o detalhe atualizado
     */
    @Transactional
    public AssessmentJourneyDetailResponse updateJourney(String journeyId, UpdateAssessmentJourneyRequest request) {
        AssessmentJourneyEntity journey = findJourney(journeyId);
        assertDraft(journey);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da jornada não pode ser vazio.");
            }
            journey.setName(request.name().trim());
        }
        if (request.description() != null) {
            journey.setDescription(trimToNull(request.description()));
        }
        journey.setUpdatedAt(Instant.now());

        AssessmentJourneyEntity saved = journeyRepository.save(journey);
        auditEventService.appendAssessmentJourneyEvent(
                saved.getTenantId(),
                currentUserService.requiredUserId(),
                saved.getId(),
                AuditEventType.ASSESSMENT_JOURNEY_UPDATED,
                "Dados básicos da jornada atualizados.",
                auditMetadata.of("name", saved.getName())
        );
        return toDetailResponse(saved);
    }

    /**
     * Adiciona um teste publicado a uma jornada em rascunho.
     *
     * <p>Só aceita simulação publicada e guarda o número da versão publicada
     * usada, garantindo que a etapa não aponta para versão em rascunho.</p>
     *
     * @param journeyId identificador da jornada
     * @param request teste a adicionar (simulação, sequência, ordem, obrigatoriedade)
     * @return o detalhe atualizado da jornada
     */
    @Transactional
    public AssessmentJourneyDetailResponse addStep(String journeyId, AddJourneyStepRequest request) {
        AssessmentJourneyEntity journey = findJourney(journeyId);
        assertDraft(journey);

        String tenantId = journey.getTenantId();
        PublishedSimulation simulation = simulationCatalogService
                .findPublishedById(tenantId, request.simulationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Só é possível adicionar uma simulação publicada à jornada."
                ));

        String sequenceKey = normalizeSequenceKey(request.sequenceKey());
        int orderIndex = resolveOrderIndex(journey, sequenceKey, request.orderIndex());
        boolean required = request.required() == null || request.required();

        AssessmentJourneyStepEntity step = new AssessmentJourneyStepEntity();
        step.setTenantId(tenantId);
        step.setJourney(journey);
        step.setSimulationId(simulation.id());
        step.setSimulationVersionNumber(simulation.versionNumber());
        step.setSequenceKey(sequenceKey);
        step.setOrderIndex(orderIndex);
        step.setRequired(required);
        step.setCreatedAt(Instant.now());
        journey.getSteps().add(step);
        journey.setUpdatedAt(Instant.now());

        journeyRepository.save(journey);
        auditEventService.appendAssessmentJourneyEvent(
                tenantId,
                currentUserService.requiredUserId(),
                journeyId,
                AuditEventType.ASSESSMENT_JOURNEY_UPDATED,
                "Teste adicionado à jornada.",
                auditMetadata.of(
                        "simulationId", simulation.id(),
                        "simulationVersionNumber", simulation.versionNumber(),
                        "sequenceKey", sequenceKey,
                        "orderIndex", orderIndex,
                        "required", required
                )
        );
        return toDetailResponse(journey);
    }

    /**
     * Atualiza a ordem, a sequência ou a obrigatoriedade de uma etapa em rascunho.
     *
     * @param journeyId identificador da jornada
     * @param stepId identificador da etapa
     * @param request campos a atualizar
     * @return o detalhe atualizado da jornada
     */
    @Transactional
    public AssessmentJourneyDetailResponse updateStep(String journeyId, Long stepId, UpdateJourneyStepRequest request) {
        AssessmentJourneyEntity journey = findJourney(journeyId);
        assertDraft(journey);
        AssessmentJourneyStepEntity step = findStep(journey, stepId);

        if (request.sequenceKey() != null) {
            step.setSequenceKey(normalizeSequenceKey(request.sequenceKey()));
        }
        if (request.orderIndex() != null) {
            if (request.orderIndex() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A ordem do teste não pode ser negativa.");
            }
            step.setOrderIndex(request.orderIndex());
        }
        if (request.required() != null) {
            step.setRequired(request.required());
        }
        assertNoDuplicateOrderWithinSequence(journey, step);
        journey.setUpdatedAt(Instant.now());

        journeyRepository.save(journey);
        auditEventService.appendAssessmentJourneyEvent(
                journey.getTenantId(),
                currentUserService.requiredUserId(),
                journeyId,
                AuditEventType.ASSESSMENT_JOURNEY_UPDATED,
                "Etapa da jornada atualizada.",
                auditMetadata.of(
                        "stepId", stepId,
                        "sequenceKey", step.getSequenceKey(),
                        "orderIndex", step.getOrderIndex(),
                        "required", step.isRequired()
                )
        );
        return toDetailResponse(journey);
    }

    /**
     * Remove uma etapa da jornada enquanto ela estiver em rascunho.
     *
     * @param journeyId identificador da jornada
     * @param stepId identificador da etapa a remover
     */
    @Transactional
    public void removeStep(String journeyId, Long stepId) {
        AssessmentJourneyEntity journey = findJourney(journeyId);
        assertDraft(journey);
        AssessmentJourneyStepEntity step = findStep(journey, stepId);

        journey.getSteps().remove(step);
        journey.setUpdatedAt(Instant.now());
        journeyRepository.save(journey);
        auditEventService.appendAssessmentJourneyEvent(
                journey.getTenantId(),
                currentUserService.requiredUserId(),
                journeyId,
                AuditEventType.ASSESSMENT_JOURNEY_UPDATED,
                "Etapa removida da jornada.",
                auditMetadata.of("stepId", stepId)
        );
    }

    /**
     * Publica a jornada, tornando a composição estável e apta a gerar links.
     *
     * <p>Valida que: a jornada tem nome; existe ao menos uma sequência; cada
     * sequência tem ao menos um teste; todos os testes têm versão publicada; e
     * não há ordem duplicada dentro da mesma sequência.</p>
     *
     * @param journeyId identificador da jornada
     * @return o detalhe da jornada publicada
     */
    @Transactional
    public AssessmentJourneyDetailResponse publishJourney(String journeyId) {
        AssessmentJourneyEntity journey = findJourney(journeyId);
        if (journey.getStatus() == AssessmentJourneyStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Jornada arquivada não pode ser publicada.");
        }
        validateForPublication(journey);

        Instant now = Instant.now();
        journey.setStatus(AssessmentJourneyStatus.PUBLISHED);
        journey.setPublishedAt(now);
        journey.setUpdatedAt(now);

        AssessmentJourneyEntity saved = journeyRepository.save(journey);
        auditEventService.appendAssessmentJourneyEvent(
                saved.getTenantId(),
                currentUserService.requiredUserId(),
                saved.getId(),
                AuditEventType.ASSESSMENT_JOURNEY_PUBLISHED,
                "Jornada de avaliação publicada.",
                auditMetadata.of(
                        "stepCount", saved.getSteps().size(),
                        "sequenceCount", distinctSequenceKeys(saved).size()
                )
        );
        return toDetailResponse(saved);
    }

    /**
     * Arquiva a jornada: deixa de gerar novos links, mas mantém o histórico.
     *
     * @param journeyId identificador da jornada
     * @return o detalhe da jornada arquivada
     */
    @Transactional
    public AssessmentJourneyDetailResponse archiveJourney(String journeyId) {
        AssessmentJourneyEntity journey = findJourney(journeyId);
        if (journey.getStatus() == AssessmentJourneyStatus.ARCHIVED) {
            return toDetailResponse(journey);
        }

        journey.setStatus(AssessmentJourneyStatus.ARCHIVED);
        journey.setUpdatedAt(Instant.now());
        AssessmentJourneyEntity saved = journeyRepository.save(journey);
        auditEventService.appendAssessmentJourneyEvent(
                saved.getTenantId(),
                currentUserService.requiredUserId(),
                saved.getId(),
                AuditEventType.ASSESSMENT_JOURNEY_ARCHIVED,
                "Jornada de avaliação arquivada.",
                auditMetadata.of("name", saved.getName())
        );
        return toDetailResponse(saved);
    }

    // ----- validação de publicação -----

    private void validateForPublication(AssessmentJourneyEntity journey) {
        if (journey.getName() == null || journey.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A jornada precisa de um nome para ser publicada.");
        }
        if (journey.getSteps().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A jornada precisa de pelo menos uma sequência com um teste.");
        }

        Map<String, List<AssessmentJourneyStepEntity>> bySequence = stepsBySequence(journey);
        for (Map.Entry<String, List<AssessmentJourneyStepEntity>> entry : bySequence.entrySet()) {
            List<AssessmentJourneyStepEntity> steps = entry.getValue();
            if (steps.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A sequência '" + entry.getKey() + "' precisa de pelo menos um teste.");
            }
            assertUniqueOrderIndices(entry.getKey(), steps);
        }

        String tenantId = journey.getTenantId();
        for (AssessmentJourneyStepEntity step : journey.getSteps()) {
            if (simulationCatalogService.findPublishedById(tenantId, step.getSimulationId()).isEmpty()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "O teste '" + step.getSimulationId() + "' não possui versão publicada."
                );
            }
        }
    }

    private void assertUniqueOrderIndices(String sequenceKey, List<AssessmentJourneyStepEntity> steps) {
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        for (AssessmentJourneyStepEntity step : steps) {
            if (!seen.add(step.getOrderIndex())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Há ordem duplicada na sequência '" + sequenceKey + "'."
                );
            }
        }
    }

    private void assertNoDuplicateOrderWithinSequence(AssessmentJourneyEntity journey, AssessmentJourneyStepEntity changed) {
        for (AssessmentJourneyStepEntity other : journey.getSteps()) {
            if (other != changed
                    && other.getSequenceKey().equals(changed.getSequenceKey())
                    && other.getOrderIndex() == changed.getOrderIndex()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Já existe um teste com essa ordem na sequência '" + changed.getSequenceKey() + "'."
                );
            }
        }
    }

    // ----- helpers de leitura / mapeamento -----

    private AssessmentJourneyEntity findJourney(String journeyId) {
        String tenantId = currentTenantService.requiredTenantId();
        return journeyRepository.findByTenantIdAndId(tenantId, journeyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jornada de avaliação não encontrada."));
    }

    private AssessmentJourneyStepEntity findStep(AssessmentJourneyEntity journey, Long stepId) {
        return journey.getSteps().stream()
                .filter(step -> step.getId() != null && step.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa da jornada não encontrada."));
    }

    private void assertDraft(AssessmentJourneyEntity journey) {
        if (journey.getStatus() != AssessmentJourneyStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Apenas jornadas em rascunho podem ser editadas. Volte para rascunho ou crie uma nova versão."
            );
        }
    }

    private String normalizeSequenceKey(String sequenceKey) {
        if (sequenceKey == null || sequenceKey.isBlank()) {
            return DEFAULT_SEQUENCE_KEY;
        }
        return sequenceKey.trim();
    }

    private int resolveOrderIndex(AssessmentJourneyEntity journey, String sequenceKey, Integer requestedOrderIndex) {
        List<AssessmentJourneyStepEntity> sequenceSteps = journey.getSteps().stream()
                .filter(step -> step.getSequenceKey().equals(sequenceKey))
                .toList();
        if (requestedOrderIndex == null) {
            return sequenceSteps.stream()
                    .mapToInt(AssessmentJourneyStepEntity::getOrderIndex)
                    .max()
                    .orElse(-1) + 1;
        }
        boolean duplicate = sequenceSteps.stream()
                .anyMatch(step -> step.getOrderIndex() == requestedOrderIndex);
        if (duplicate) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um teste com essa ordem na sequência '" + sequenceKey + "'."
            );
        }
        return requestedOrderIndex;
    }

    private Map<String, List<AssessmentJourneyStepEntity>> stepsBySequence(AssessmentJourneyEntity journey) {
        Map<String, List<AssessmentJourneyStepEntity>> bySequence = new LinkedHashMap<>();
        journey.getSteps().stream()
                .sorted(Comparator.comparing(AssessmentJourneyStepEntity::getSequenceKey)
                        .thenComparingInt(AssessmentJourneyStepEntity::getOrderIndex))
                .forEach(step -> bySequence
                        .computeIfAbsent(step.getSequenceKey(), key -> new ArrayList<>())
                        .add(step));
        return bySequence;
    }

    private java.util.Set<String> distinctSequenceKeys(AssessmentJourneyEntity journey) {
        java.util.Set<String> keys = new java.util.LinkedHashSet<>();
        journey.getSteps().forEach(step -> keys.add(step.getSequenceKey()));
        return keys;
    }

    private AssessmentJourneySummaryResponse toSummaryResponse(AssessmentJourneyEntity journey) {
        return new AssessmentJourneySummaryResponse(
                journey.getId(),
                journey.getName(),
                journey.getDescription(),
                journey.getStatus(),
                journey.getSteps().size(),
                distinctSequenceKeys(journey).size(),
                journey.getCreatedAt(),
                journey.getUpdatedAt()
        );
    }

    private AssessmentJourneyDetailResponse toDetailResponse(AssessmentJourneyEntity journey) {
        Map<String, String> nameCache = new LinkedHashMap<>();
        List<AssessmentJourneyDetailResponse.SequenceResponse> sequences = new ArrayList<>();
        stepsBySequence(journey).forEach((sequenceKey, steps) -> {
            List<JourneyStepResponse> stepResponses = steps.stream()
                    .map(step -> toStepResponse(journey.getTenantId(), step, nameCache))
                    .toList();
            sequences.add(new AssessmentJourneyDetailResponse.SequenceResponse(sequenceKey, stepResponses));
        });

        return new AssessmentJourneyDetailResponse(
                journey.getId(),
                journey.getName(),
                journey.getDescription(),
                journey.getStatus(),
                journey.getCreatedAt(),
                journey.getUpdatedAt(),
                journey.getPublishedAt(),
                sequences
        );
    }

    private JourneyStepResponse toStepResponse(String tenantId, AssessmentJourneyStepEntity step, Map<String, String> nameCache) {
        String simulationName = nameCache.computeIfAbsent(step.getSimulationId(), id ->
                simulationCatalogService.findPublishedById(tenantId, id)
                        .map(PublishedSimulation::name)
                        .orElse(id));
        return new JourneyStepResponse(
                step.getId(),
                step.getSimulationId(),
                simulationName,
                step.getSimulationVersionNumber(),
                step.getSequenceKey(),
                step.getOrderIndex(),
                step.isRequired()
        );
    }

    private String generateJourneyId(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        String base = normalized.isBlank() ? "jornada" : normalized;
        if (base.length() > 80) {
            base = base.substring(0, 80).replaceAll("-$", "");
        }

        String id;
        do {
            id = base + "-" + UUID.randomUUID().toString().substring(0, 8);
        } while (journeyRepository.existsById(id));
        return id;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Acesso interno para a camada de tentativas localizar a jornada do tenant. */
    @Transactional(readOnly = true)
    public Optional<AssessmentJourneyEntity> findJourneyForTenant(String tenantId, String journeyId) {
        return journeyRepository.findByTenantIdAndId(tenantId, journeyId);
    }
}
