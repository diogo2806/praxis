package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.candidate.dto.ReviewRequest;
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

/**
 * Registra o pedido de revisão humana feito pelo candidato (REQ-L5 / LGPD art. 20). O candidato
 * acessa pelo próprio link (rota pública, sem tenant no contexto): resolvemos o tenant a partir da
 * tentativa e gravamos o pedido na trilha append-only, de onde o recrutador o vê (e ele entra no
 * relatório de evidência). A revisão em si é a decisão humana já garantida pelo REQ-L1.
 */
@Service
public class CandidateReviewRequestService {

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public CandidateReviewRequestService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void register(String attemptId, ReviewRequest request) {
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));

        Instant requestedAt = Instant.now();
        String reason = normalizeReason(request == null ? null : request.reason());

        auditEventService.appendCandidateAttemptEvent(
                attempt.getTenantId(),
                attemptId,
                AuditEventType.REVIEW_REQUESTED,
                "Revisão humana solicitada pelo candidato.",
                buildMetadata(attemptId, reason, requestedAt)
        );
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildMetadata(String attemptId, String reason, Instant requestedAt) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("attemptId", attemptId);
        payload.put("source", "candidate");
        payload.put("reason", reason);
        payload.put("requestedAt", requestedAt.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao registrar o pedido de revisão.", exception);
        }
    }
}
