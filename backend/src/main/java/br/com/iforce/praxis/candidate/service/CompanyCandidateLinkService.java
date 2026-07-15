package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.billing.service.CreditService;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.CreateDirectCandidateLinkRequest;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.model.ReliabilityLevel;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import br.com.iforce.praxis.gupy.model.ResultItem;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.CandidateAttemptMapper;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.IdempotencyKeyHasher;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.security.EmpresaSecurity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CompanyCandidateLinkService {

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final CandidateAttemptMapper candidateAttemptMapper;
    private final CandidateAttemptService candidateAttemptService;
    private final CreditService creditService;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;

    public CompanyCandidateLinkService(
            CandidateAttemptRepository candidateAttemptRepository,
            SimulationCatalogService simulationCatalogService,
            CandidateAttemptMapper candidateAttemptMapper,
            CandidateAttemptService candidateAttemptService,
            CreditService creditService,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.candidateAttemptMapper = candidateAttemptMapper;
        this.candidateAttemptService = candidateAttemptService;
        this.creditService = creditService;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
    }

    /**
     * Cria uma nova aplicação. Repetições com o mesmo applicationCycleId são idempotentes;
     * outro ciclo cria outra tentativa mesmo para a mesma pessoa e avaliação.
     */
    @Transactional
    public CreateCandidateLinkResponse createNewAttempt(CreateDirectCandidateLinkRequest request) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        PublishedSimulation simulation = simulationCatalogService
                .findPublishedById(empresaId, request.simulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos o teste publicado."));

        String applicationCycleId = normalizeApplicationCycleId(request.applicationCycleId());
        String candidateEmail = request.candidateEmail().trim().toLowerCase(Locale.ROOT);
        String idempotencyKey = IdempotencyKeyHasher.sha256Hex(
                empresaId + "|company-direct|" + candidateEmail + "|" + request.simulationId().trim()
                        + "|cycle|" + applicationCycleId
        );

        CandidateAttemptEntity entity = candidateAttemptRepository
                .findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)
                .orElseGet(() -> createAttemptSafely(
                        empresaId,
                        idempotencyKey,
                        applicationCycleId,
                        candidateEmail,
                        request,
                        simulation
                ));

        return toResponse(entity, simulation);
    }

    /**
     * Recupera o mesmo link de uma tentativa já existente. A busca é sempre limitada à empresa
     * autenticada e nunca cria ou consome uma nova tentativa.
     */
    @Transactional
    public CreateCandidateLinkResponse resendExistingLink(String attemptId) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        CandidateAttemptEntity entity = candidateAttemptRepository
                .findByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        PublishedSimulation simulation = findSimulation(entity);

        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                entity.getId(),
                AuditEventType.ATTEMPT_LINK_RESENT,
                "Link existente preparado para reenvio, sem criar nova tentativa.",
                auditMetadata.of(
                        "simulationId", entity.getSimulationId(),
                        "applicationCycleId", entity.getApplicationCycleId(),
                        "status", entity.getStatus()
                )
        );

        return toResponse(entity, simulation);
    }

    private CandidateAttemptEntity createAttemptSafely(
            String empresaId,
            String idempotencyKey,
            String applicationCycleId,
            String candidateEmail,
            CreateDirectCandidateLinkRequest request,
            PublishedSimulation simulation
    ) {
        creditService.assertCanStartNewAttempt(empresaId);
        try {
            CandidateAttempt attempt = new CandidateAttempt(
                    "att_" + randomToken(),
                    "res_" + randomToken(),
                    empresaId,
                    empresaId,
                    simulation.id(),
                    simulation.versionId(),
                    simulation.versionNumber(),
                    idempotencyKey,
                    request.candidateName().trim(),
                    candidateEmail,
                    AttemptStatus.NOT_STARTED,
                    null,
                    simulation.competencies().stream()
                            .map(competency -> new ResultItem(
                                    competency,
                                    0,
                                    simulation.competencyTiers().getOrDefault(competency, ResultTier.MAJOR)
                            ))
                            .toList(),
                    Map.of(),
                    Map.of(),
                    ResultDecision.IN_PROGRESS,
                    false,
                    ReliabilityLevel.NORMAL,
                    normalizeAccommodationMultiplier(request.accommodationTimeMultiplier()),
                    "Resultado ainda não finalizado.",
                    Instant.now(),
                    null,
                    null
            );

            CandidateAttemptEntity entity = new CandidateAttemptEntity();
            candidateAttemptMapper.applyDomainToEntity(attempt, entity);
            entity.setApplicationCycleId(applicationCycleId);
            CandidateAttemptEntity saved = candidateAttemptRepository.save(entity);

            auditEventService.appendCandidateAttemptEvent(
                    empresaId,
                    saved.getId(),
                    AuditEventType.ATTEMPT_CREATED,
                    "Nova tentativa criada pela empresa para envio direto ao candidato.",
                    auditMetadata.of(
                            "simulationId", request.simulationId(),
                            "candidateEmail", candidateEmail,
                            "applicationCycleId", applicationCycleId
                    )
            );
            return saved;
        } catch (DataIntegrityViolationException exception) {
            return candidateAttemptRepository.findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)
                    .orElseThrow(() -> exception);
        }
    }

    private PublishedSimulation findSimulation(CandidateAttemptEntity entity) {
        if (entity.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(entity.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos esta versão do teste."));
        }
        return simulationCatalogService.findPublishedById(entity.getEmpresaId(), entity.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos o teste publicado."));
    }

    private CreateCandidateLinkResponse toResponse(CandidateAttemptEntity entity, PublishedSimulation simulation) {
        return new CreateCandidateLinkResponse(
                entity.getId(),
                candidateAttemptService.candidatePageUrlFor(entity.getEmpresaId(), entity.getId()),
                simulation.name()
        );
    }

    private String normalizeApplicationCycleId(String value) {
        if (value == null || value.isBlank()) {
            return "generated-" + randomToken();
        }
        return value.trim();
    }

    private BigDecimal normalizeAccommodationMultiplier(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ONE) < 0) {
            return BigDecimal.ONE;
        }
        if (value.compareTo(BigDecimal.valueOf(9.99)) > 0) {
            return BigDecimal.valueOf(9.99);
        }
        return value;
    }

    private String randomToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
