package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.billing.service.CreditService;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
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
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Controla o ciclo de vida dos links criados diretamente pela empresa.
 *
 * <p>Uma nova aplicação é identificada por {@code applicationCycleId}. Repetir a mesma
 * requisição reaproveita apenas a tentativa daquele ciclo, enquanto o reenvio usa o
 * identificador da tentativa e nunca cria ou cobra uma nova aplicação.</p>
 */
@Service
public class CompanyCandidateLinkService {

    public static final String CREATED_NEW_APPLICATION = "CREATED_NEW_APPLICATION";
    public static final String REUSED_IDEMPOTENT_REQUEST = "REUSED_IDEMPOTENT_REQUEST";
    public static final String RESENT_EXISTING_LINK = "RESENT_EXISTING_LINK";
    public static final String EXTENDED_LINK_VALIDITY = "EXTENDED_LINK_VALIDITY";
    private static final int REQUEST_FINGERPRINT_VERSION = 1;
    private static final int MAX_EXTENSION_DAYS = 365;

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
     * Cria uma nova tentativa para um ciclo de aplicação ou reaproveita somente uma
     * repetição idempotente do mesmo ciclo.
     */
    @Transactional
    public CreateCandidateLinkResponse createNewApplication(CreateCandidateLinkRequest request) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        String applicationCycleId = requiredApplicationCycleId(request.applicationCycleId());
        String normalizedEmail = normalizeEmail(request.candidateEmail());
        PublishedSimulation publishedSimulation = simulationCatalogService
                .findPublishedById(empresaId, request.simulationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Não encontramos o teste publicado."
                ));

        String idempotencyKey = IdempotencyKeyHasher.sha256Hex(
                empresaId
                        + "|company-application|"
                        + normalizedEmail
                        + "|"
                        + request.simulationId().trim()
                        + "|"
                        + applicationCycleId
        );
        String requestFingerprint = requestFingerprint(
                request,
                applicationCycleId,
                normalizedEmail,
                publishedSimulation
        );

        CompanyLinkResolution resolution = resolveApplication(
                empresaId,
                idempotencyKey,
                requestFingerprint,
                applicationCycleId,
                request,
                publishedSimulation
        );

        return response(
                empresaId,
                resolution.entity(),
                publishedSimulation,
                resolution.reused(),
                resolution.reused() ? REUSED_IDEMPOTENT_REQUEST : CREATED_NEW_APPLICATION
        );
    }

    /**
     * Reenvia exatamente o link vigente de uma tentativa existente, sem gerar tentativa ou
     * consumir crédito adicional. Links expirados precisam ser reativados explicitamente.
     */
    @Transactional
    public CreateCandidateLinkResponse resendExisting(String attemptId) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        CandidateAttemptEntity entity = candidateAttemptRepository
                .findByEmpresaIdAndId(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        assertLinkActive(entity);
        PublishedSimulation simulation = findSimulation(entity);

        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                entity.getId(),
                AuditEventType.CANDIDATE_LINK_RESENT,
                "Link vigente reenviado pela empresa sem criar uma nova tentativa.",
                auditMetadata.of(
                        "simulationId", entity.getSimulationId(),
                        "candidateEmail", entity.getCandidateEmail(),
                        "linkExpiresAt", entity.getCandidateTokenExpiresAt()
                )
        );

        return response(empresaId, entity, simulation, true, RESENT_EXISTING_LINK);
    }

    /**
     * Acrescenta dias à validade vigente. Se o link já expirou, a nova janela começa agora;
     * se ainda está ativo, os dias são somados ao vencimento atual.
     */
    @Transactional
    public CreateCandidateLinkResponse extendValidity(String attemptId, int additionalDays) {
        validateAdditionalDays(additionalDays);
        String empresaId = EmpresaSecurity.requiredEmpresa();
        CandidateAttemptEntity entity = candidateAttemptRepository
                .findByEmpresaIdAndIdForUpdate(empresaId, attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tentativa não encontrada."));
        PublishedSimulation simulation = findSimulation(entity);

        Instant now = Instant.now();
        Instant previousExpiration = requiredExpiration(entity);
        Instant extensionBase = previousExpiration.isAfter(now) ? previousExpiration : now;
        Instant newExpiration = extensionBase.plus(additionalDays, ChronoUnit.DAYS);

        entity.setCandidateTokenIssuedAt(now);
        entity.setCandidateTokenExpiresAt(newExpiration);
        entity = candidateAttemptRepository.saveAndFlush(entity);

        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                entity.getId(),
                AuditEventType.CANDIDATE_LINK_EXTENDED,
                "Validade do link do candidato ampliada pela empresa.",
                auditMetadata.of(
                        "simulationId", entity.getSimulationId(),
                        "candidateEmail", entity.getCandidateEmail(),
                        "additionalDays", additionalDays,
                        "previousExpiration", previousExpiration,
                        "newExpiration", newExpiration,
                        "reactivated", !previousExpiration.isAfter(now)
                )
        );

        return response(empresaId, entity, simulation, true, EXTENDED_LINK_VALIDITY);
    }

    private CompanyLinkResolution resolveApplication(
            String empresaId,
            String idempotencyKey,
            String requestFingerprint,
            String applicationCycleId,
            CreateCandidateLinkRequest request,
            PublishedSimulation publishedSimulation
    ) {
        CandidateAttemptEntity existing = candidateAttemptRepository
                .findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            assertEquivalentRequest(existing, requestFingerprint);
            return new CompanyLinkResolution(existing, true);
        }

        creditService.assertCanStartNewAttempt(empresaId);
        try {
            CandidateAttemptEntity created = createAttempt(
                    empresaId,
                    idempotencyKey,
                    requestFingerprint,
                    applicationCycleId,
                    request,
                    publishedSimulation
            );
            return new CompanyLinkResolution(created, false);
        } catch (DataIntegrityViolationException exception) {
            CandidateAttemptEntity concurrent = candidateAttemptRepository
                    .findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)
                    .orElseThrow(() -> exception);
            assertEquivalentRequest(concurrent, requestFingerprint);
            return new CompanyLinkResolution(concurrent, true);
        }
    }

    private CandidateAttemptEntity createAttempt(
            String empresaId,
            String idempotencyKey,
            String requestFingerprint,
            String applicationCycleId,
            CreateCandidateLinkRequest request,
            PublishedSimulation publishedSimulation
    ) {
        CandidateAttempt attempt = new CandidateAttempt(
                "att_" + UUID.randomUUID().toString().replace("-", ""),
                "res_" + UUID.randomUUID().toString().replace("-", ""),
                empresaId,
                empresaId,
                publishedSimulation.id(),
                publishedSimulation.versionId(),
                publishedSimulation.versionNumber(),
                idempotencyKey,
                request.candidateName().trim(),
                request.candidateEmail().trim(),
                AttemptStatus.NOT_STARTED,
                null,
                publishedSimulation.competencies().stream()
                        .map(competency -> new ResultItem(
                                competency,
                                0,
                                publishedSimulation.competencyTiers().getOrDefault(competency, ResultTier.MAJOR)
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
        entity.setRequestFingerprint(requestFingerprint);
        entity.setRequestFingerprintVersion(REQUEST_FINGERPRINT_VERSION);
        CandidateAttemptEntity saved = candidateAttemptRepository.saveAndFlush(entity);

        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                saved.getId(),
                AuditEventType.ATTEMPT_CREATED,
                "Nova aplicação criada pela empresa para envio direto ao candidato.",
                auditMetadata.of(
                        "simulationId", request.simulationId(),
                        "candidateEmail", request.candidateEmail().trim(),
                        "applicationCycleId", applicationCycleId,
                        "applicationContext", normalizedContext(request.applicationContext()),
                        "simulationVersionId", publishedSimulation.versionId(),
                        "simulationVersionNumber", publishedSimulation.versionNumber(),
                        "linkExpiresAt", saved.getCandidateTokenExpiresAt()
                )
        );
        return saved;
    }

    private void assertEquivalentRequest(CandidateAttemptEntity existing, String requestFingerprint) {
        if (existing.getRequestFingerprintVersion() == null
                || existing.getRequestFingerprintVersion() != REQUEST_FINGERPRINT_VERSION
                || !requestFingerprint.equals(existing.getRequestFingerprint())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "applicationCycleId já foi usado com dados diferentes. Informe um novo ciclo para criar outra aplicação."
            );
        }
    }

    private String requestFingerprint(
            CreateCandidateLinkRequest request,
            String applicationCycleId,
            String normalizedEmail,
            PublishedSimulation publishedSimulation
    ) {
        BigDecimal multiplier = normalizeAccommodationMultiplier(request.accommodationTimeMultiplier());
        String source = REQUEST_FINGERPRINT_VERSION
                + "|"
                + request.simulationId().trim()
                + "|"
                + publishedSimulation.versionId()
                + "|"
                + request.candidateName().trim()
                + "|"
                + normalizedEmail
                + "|"
                + applicationCycleId
                + "|"
                + normalizedContext(request.applicationContext())
                + "|"
                + multiplier.stripTrailingZeros().toPlainString();
        return IdempotencyKeyHasher.sha256Hex(source);
    }

    private CreateCandidateLinkResponse response(
            String empresaId,
            CandidateAttemptEntity entity,
            PublishedSimulation simulation,
            boolean reused,
            String operation
    ) {
        return new CreateCandidateLinkResponse(
                entity.getId(),
                candidateAttemptService.candidatePageUrlFor(empresaId, entity.getId()),
                simulation.name(),
                reused,
                operation
        );
    }

    private PublishedSimulation findSimulation(CandidateAttemptEntity entity) {
        if (entity.getSimulationVersionId() != null) {
            return simulationCatalogService.findByVersionId(entity.getSimulationVersionId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Não encontramos esta versão do teste."
                    ));
        }
        return simulationCatalogService.findPublishedById(entity.getEmpresaId(), entity.getSimulationId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Não encontramos o teste publicado."
                ));
    }

    private void assertLinkActive(CandidateAttemptEntity entity) {
        Instant expiration = requiredExpiration(entity);
        if (!expiration.isAfter(Instant.now())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O link está expirado. Adicione dias de validade para reativá-lo antes de reenviar."
            );
        }
    }

    private Instant requiredExpiration(CandidateAttemptEntity entity) {
        if (entity.getCandidateTokenExpiresAt() == null) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "A validade do link da tentativa não foi encontrada."
            );
        }
        return entity.getCandidateTokenExpiresAt();
    }

    private void validateAdditionalDays(int additionalDays) {
        if (additionalDays < 1 || additionalDays > MAX_EXTENSION_DAYS) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Informe uma quantidade entre 1 e 365 dias."
            );
        }
    }

    private String requiredApplicationCycleId(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "applicationCycleId é obrigatório para criar uma nova aplicação."
            );
        }
        return value.trim();
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizedContext(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
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

    private record CompanyLinkResolution(CandidateAttemptEntity entity, boolean reused) {
    }
}
