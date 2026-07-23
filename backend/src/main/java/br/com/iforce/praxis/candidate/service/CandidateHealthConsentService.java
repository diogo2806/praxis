package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.HealthVerticalService;
import br.com.iforce.praxis.candidate.dto.HealthConsentRequest;
import br.com.iforce.praxis.candidate.dto.HealthConsentStatusResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Mantém e exige o consentimento específico da vertical educativa de saúde. */
@Service
public class CandidateHealthConsentService {

    private static final String SUBJECT_TYPE_DATA_SUBJECT = "DATA_SUBJECT";
    private static final String CONSENT_SOURCE = "CANDIDATE_PORTAL";

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;
    private final CandidateAttemptTokenResolver tokenResolver;
    private final HealthVerticalService healthVerticalService;
    private final String currentNoticeVersion;

    public CandidateHealthConsentService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper,
            CandidateAttemptTokenResolver tokenResolver,
            HealthVerticalService healthVerticalService,
            @Value("${praxis.health-consent.notice-version:2026-06-01}") String currentNoticeVersion
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
        this.tokenResolver = tokenResolver;
        this.healthVerticalService = healthVerticalService;
        this.currentNoticeVersion = normalizeVersion(currentNoticeVersion);
    }

    @Transactional(readOnly = true)
    public HealthConsentStatusResponse getStatus(String attemptToken) {
        CandidateAttemptEntity attempt = resolveAttempt(attemptToken);
        boolean healthVertical = healthVerticalService.isHealthVertical(attempt.getEmpresaId());
        boolean required = healthVertical && !isTerminal(attempt.getStatus());
        return new HealthConsentStatusResponse(
                healthVertical,
                required,
                healthVertical && hasValidCurrentConsent(attempt),
                healthVertical ? currentNoticeVersion : null
        );
    }

    @Transactional
    public void register(String attemptToken, HealthConsentRequest request) {
        validateRequest(request);
        CandidateAttemptEntity attempt = resolveAttemptForUpdate(attemptToken);
        assertHealthVertical(attempt);

        String requestedVersion = request.version().trim();
        if (!currentNoticeVersion.equals(requestedVersion)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O termo de consentimento de saúde mudou. Recarregue a página antes de continuar."
            );
        }
        if (hasValidCurrentConsent(attempt)) {
            return;
        }

        Instant consentedAt = Instant.now();
        attempt.setHealthConsentRecordedAt(consentedAt);
        attempt.setHealthConsentVersion(currentNoticeVersion);
        attempt.setHealthConsentSubjectType(SUBJECT_TYPE_DATA_SUBJECT);
        attempt.setHealthConsentSource(CONSENT_SOURCE);
        attempt.setHealthConsentRevokedAt(null);
        candidateAttemptRepository.save(attempt);

        auditEventService.appendCandidateAttemptEvent(
                attempt.getEmpresaId(),
                attempt.getId(),
                AuditEventType.HEALTH_CONSENT_RECORDED,
                "Consentimento específico de saúde registrado pelo titular.",
                buildMetadata(attempt, consentedAt, null)
        );
    }

    /**
     * Bloqueia a linha da participação até o final da operação pública. O controlador
     * abre a transação antes desta chamada para impedir corrida entre revogação,
     * carregamento da etapa e registro da resposta.
     */
    @Transactional
    public void assertConsentGranted(String attemptToken) {
        CandidateAttemptEntity attempt = resolveAttemptForUpdate(attemptToken);
        if (!healthVerticalService.isHealthVertical(attempt.getEmpresaId()) || isTerminal(attempt.getStatus())) {
            return;
        }
        if (!hasValidCurrentConsent(attempt)) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "Registre o consentimento específico de saúde antes de iniciar ou continuar a atividade."
            );
        }
    }

    @Transactional
    public void revoke(String attemptToken) {
        CandidateAttemptEntity attempt = resolveAttemptForUpdate(attemptToken);
        assertHealthVertical(attempt);
        if (attempt.getHealthConsentRecordedAt() == null || attempt.getHealthConsentRevokedAt() != null) {
            return;
        }

        Instant revokedAt = Instant.now();
        attempt.setHealthConsentRevokedAt(revokedAt);
        candidateAttemptRepository.save(attempt);

        auditEventService.appendCandidateAttemptEvent(
                attempt.getEmpresaId(),
                attempt.getId(),
                AuditEventType.HEALTH_CONSENT_REVOKED,
                "Consentimento específico de saúde revogado pelo titular.",
                buildMetadata(attempt, attempt.getHealthConsentRecordedAt(), revokedAt)
        );
    }

    private void validateRequest(HealthConsentRequest request) {
        if (request == null || request.version() == null || request.version().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A versão do consentimento é obrigatória.");
        }
        if (request.onBehalfOfMinor()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Participações de menor ou vulnerável permanecem bloqueadas até existir identificação validada do responsável legal."
            );
        }
    }

    private void assertHealthVertical(CandidateAttemptEntity attempt) {
        if (!healthVerticalService.isHealthVertical(attempt.getEmpresaId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A vertical de saúde não possui aprovação formal para esta empresa."
            );
        }
    }

    private boolean hasValidCurrentConsent(CandidateAttemptEntity attempt) {
        return attempt.getHealthConsentRecordedAt() != null
                && currentNoticeVersion.equals(attempt.getHealthConsentVersion())
                && SUBJECT_TYPE_DATA_SUBJECT.equals(attempt.getHealthConsentSubjectType())
                && CONSENT_SOURCE.equals(attempt.getHealthConsentSource())
                && attempt.getHealthConsentRevokedAt() == null;
    }

    private boolean isTerminal(AttemptStatus status) {
        return status == AttemptStatus.COMPLETED
                || status == AttemptStatus.ABANDONED
                || status == AttemptStatus.EXPIRED;
    }

    private CandidateAttemptEntity resolveAttemptForUpdate(String attemptToken) {
        CandidateAttemptEntity attempt = resolveAttempt(attemptToken);
        return candidateAttemptRepository.findByEmpresaIdAndIdForUpdate(attempt.getEmpresaId(), attempt.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));
    }

    private CandidateAttemptEntity resolveAttempt(String attemptToken) {
        CandidateAttemptTokenResolver.ResolvedAttemptToken resolved = tokenResolver.resolve(attemptToken);
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(resolved.attemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));
        if (resolved.empresaId() != null && !resolved.empresaId().equals(attempt.getEmpresaId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token não pertence a esta participação.");
        }
        return attempt;
    }

    private String buildMetadata(CandidateAttemptEntity attempt, Instant consentedAt, Instant revokedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attemptId", attempt.getId());
        payload.put("source", attempt.getHealthConsentSource());
        payload.put("subjectType", attempt.getHealthConsentSubjectType());
        payload.put("noticeVersion", attempt.getHealthConsentVersion());
        payload.put("active", revokedAt == null);
        payload.put("consentedAt", consentedAt == null ? null : consentedAt.toString());
        payload.put("revokedAt", revokedAt == null ? null : revokedAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar o consentimento.", exception);
        }
    }

    private static String normalizeVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("A versão vigente do consentimento de saúde deve ser configurada.");
        }
        return version.trim();
    }
}
