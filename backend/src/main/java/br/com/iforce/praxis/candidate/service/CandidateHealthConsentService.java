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

/** Registra o consentimento específico da vertical educativa de saúde. */
@Service
public class CandidateHealthConsentService {

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

        CandidateAttemptTokenResolver.ResolvedAttemptToken resolved = tokenResolver.resolve(attemptToken);
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(resolved.attemptId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));
        if (resolved.empresaId() != null && !resolved.empresaId().equals(attempt.getEmpresaId())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token não pertence a esta participação.");
        }
        if (!healthVerticalService.isHealthVertical(attempt.getEmpresaId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A vertical de saúde não possui aprovação formal para esta empresa."
            );
        }

        Instant consentedAt = Instant.now();
        auditEventService.appendCandidateAttemptEvent(
                attempt.getEmpresaId(),
                attempt.getId(),
                AuditEventType.HEALTH_CONSENT_RECORDED,
                "Consentimento de saúde registrado pelo participante.",
                buildMetadata(attempt.getId(), request, consentedAt)
        );
    }

    private String buildMetadata(String attemptId, HealthConsentRequest request, Instant consentedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attemptId", attemptId);
        payload.put("source", "candidate");
        payload.put("noticeVersion", request.version().trim());
        payload.put("onBehalfOfMinor", false);
        payload.put("consentedAt", consentedAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar o consentimento.", exception);
        }
    }
}
