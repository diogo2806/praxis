package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.candidate.dto.ReviewRequest;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.privacy.model.ComplianceRequestStatus;
import br.com.iforce.praxis.shared.privacy.persistence.entity.HumanReviewRequestEntity;
import br.com.iforce.praxis.shared.privacy.persistence.repository.HumanReviewRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Registra e acompanha a revisão humana prevista no art. 20 da LGPD. */
@Service
public class CandidateReviewRequestService {

    private static final List<ComplianceRequestStatus> OPEN_STATUSES = List.of(
            ComplianceRequestStatus.PENDING,
            ComplianceRequestStatus.IN_PROGRESS
    );

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;
    private final HumanReviewRequestRepository humanReviewRequestRepository;
    private final int deadlineDays;

    public CandidateReviewRequestService(
            CandidateAttemptRepository candidateAttemptRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper,
            HumanReviewRequestRepository humanReviewRequestRepository,
            @Value("${praxis.human-review-deadline-days:5}") int deadlineDays
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
        this.humanReviewRequestRepository = humanReviewRequestRepository;
        this.deadlineDays = Math.max(1, deadlineDays);
    }

    @Transactional
    public String register(String attemptId, ReviewRequest request) {
        CandidateAttemptEntity attempt = candidateAttemptRepository.findById(attemptId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada."));

        HumanReviewRequestEntity existing = humanReviewRequestRepository
                .findFirstByAttemptIdAndStatusInOrderByRequestedAtDesc(attemptId, OPEN_STATUSES)
                .orElse(null);
        if (existing != null) {
            return existing.getId();
        }

        Instant requestedAt = Instant.now();
        HumanReviewRequestEntity entity = new HumanReviewRequestEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setEmpresaId(attempt.getEmpresaId());
        entity.setAttemptId(attemptId);
        entity.setReason(normalizeReason(request == null ? null : request.reason()));
        entity.setStatus(ComplianceRequestStatus.PENDING);
        entity.setRequestedAt(requestedAt);
        entity.setDueAt(requestedAt.plus(deadlineDays, ChronoUnit.DAYS));
        entity.setCreatedAt(requestedAt);
        entity.setUpdatedAt(requestedAt);
        humanReviewRequestRepository.save(entity);

        attempt.setHumanReviewRequired(true);
        attempt.setHumanReviewCompletedAt(null);
        attempt.setHumanReviewedBy(null);
        attempt.setHumanReviewResolution(null);
        candidateAttemptRepository.save(attempt);

        auditEventService.appendCandidateAttemptEvent(
                attempt.getEmpresaId(),
                attemptId,
                AuditEventType.REVIEW_REQUESTED,
                "Revisão humana solicitada pelo candidato.",
                buildMetadata(entity)
        );
        return entity.getId();
    }

    private String normalizeReason(String reason) {
        if (reason == null) {
            return null;
        }
        String trimmed = reason.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String buildMetadata(HumanReviewRequestEntity entity) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("reviewRequestId", entity.getId());
        payload.put("attemptId", entity.getAttemptId());
        payload.put("source", "candidate");
        payload.put("status", entity.getStatus().name());
        payload.put("requestedAt", entity.getRequestedAt().toString());
        payload.put("dueAt", entity.getDueAt().toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao registrar o pedido de revisão.",
                    exception
            );
        }
    }
}
