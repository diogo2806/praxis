package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.HealthVerticalService;
import br.com.iforce.praxis.candidate.dto.HealthConsentRequest;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Registra e valida o consentimento específico da vertical educativa de saúde. */
@Service
public class CandidateHealthConsentService {

    private static final String SUBJECT_TYPE_DATA_SUBJECT = "DATA_SUBJECT";

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;
    private final CandidateAttemptTokenResolver tokenResolver;
    private final HealthVerticalService healthVerticalService;

    public CandidateHealthConsentService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper,
            CandidateAttemptTokenResolver tokenResolver,
            HealthVerticalService healthVerticalService
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
        this.tokenResolver = tokenResolver;
        this.healthVerticalService = healthVerticalService;
    }

    @Transactional
    public void register(String attemptToken, HealthConsentRequest request) {
        if (request == null || request.version() == null || request.version().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A versão do consentimento é obrigatória.");
        }
        if (request.onBehalfOfMinor()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Participações de menor ou vulnerável permanecem bloqueadas até existir identificação validada do responsável legal."
            );
        }

        CandidateAttemptEntity attempt = resolveAttempt(attemptToken);
        if (!healthVerticalService.isHealthVertical(attempt.getEmpresaId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A vertical de saúde não possui aprovação formal para esta empresa."
            );
        }

        Instant consentedAt = Instant.now();
        String version = request.version().trim();
        attempt.setHealthConsentRecordedAt(consentedAt);
        attempt.setHealthConsentVersion(version);
        attempt.setHealthConsentSubjectType(SUBJECT_TYPE_DATA_SUBJECT);
        attempt.setHealthConsentRevokedAt(null);
        candidateAttemptRepository.save(attempt);

        auditEventService.appendCandidateAttemptEvent(
                attempt.getEmpresaId(),
                attempt.getId(),
                AuditEventType.HEALTH_CONSENT_RECORDED,
                "Consentimento de saúde registrado pelo participante.",
                buildMetadata(attempt.getId(), version, consentedAt)
        );
    }

    /**
     * Impede que dados da avaliação de saúde sejam carregados ou respondidos sem um consentimento
     * persistido, versionado e não revogado. Empresas fora da vertical de saúde não são afetadas.
     */
    @Transactional(readOnly = true)
    public void assertConsentGranted(String attemptToken) {
        CandidateAttemptEntity attempt = resolveAttempt(attemptToken);
        if (!healthVerticalService.isHealthVertical(attempt.getEmpresaId())) {
            return;
        }

        boolean valid = attempt.getHealthConsentRecordedAt() != null
                && attempt.getHealthConsentVersion() != null
                && !attempt.getHealthConsentVersion().isBlank()
                && SUBJECT_TYPE_DATA_SUBJECT.equals(attempt.getHealthConsentSubjectType())
                && attempt.getHealthConsentRevokedAt() == null;
        if (!valid) {
            throw new ResponseStatusException(
                    HttpStatus.PRECONDITION_REQUIRED,
                    "O consentimento específico para tratamento de dados de saúde deve ser registrado antes da avaliação."
            );
        }
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

    private String buildMetadata(String attemptId, String version, Instant consentedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attemptId", attemptId);
        payload.put("source", "candidate");
        payload.put("noticeVersion", version);
        payload.put("subjectType", SUBJECT_TYPE_DATA_SUBJECT);
        payload.put("onBehalfOfMinor", false);
        payload.put("consentedAt", consentedAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar o consentimento.", exception);
        }
    }
}
