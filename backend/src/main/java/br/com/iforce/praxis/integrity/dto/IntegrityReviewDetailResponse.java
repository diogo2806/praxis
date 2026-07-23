package br.com.iforce.praxis.integrity.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.integrity.model.IntegrityEventType;
import br.com.iforce.praxis.integrity.model.IntegrityInputMode;
import br.com.iforce.praxis.integrity.model.IntegrityReviewAuditAction;
import br.com.iforce.praxis.integrity.model.IntegrityReviewDecision;
import br.com.iforce.praxis.integrity.model.IntegrityReviewStatus;
import br.com.iforce.praxis.integrity.model.IntegritySessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Detalhe restrito da revisão técnica, com evidências e auditoria de acesso.")
public record IntegrityReviewDetailResponse(
        String attemptId,
        String candidateName,
        String candidateEmail,
        AttemptStatus attemptStatus,
        IntegrityReviewStatus reviewStatus,
        IntegrityReviewDecision decision,
        String justification,
        boolean sharedWithCompany,
        String reviewedBy,
        Instant decidedAt,
        Instant retentionUntil,
        Instant evidenceDiscardedAt,
        List<Alert> alerts,
        List<Session> sessions,
        List<TimelineEvent> events,
        List<AuditEntry> auditTrail
) {

    public record Alert(String code, String title, String explanation, long occurrences) {
    }

    public record Session(
            String id,
            IntegritySessionStatus status,
            Instant startedAt,
            Instant lastHeartbeatAt,
            Instant closedAt,
            String userAgentCategory,
            IntegrityInputMode inputMode
    ) {
    }

    public record TimelineEvent(
            Long id,
            String sessionId,
            IntegrityEventType eventType,
            Instant occurredAt,
            Instant receivedAt,
            IntegrityInputMode inputMode,
            String visibilityState,
            Long sequenceNumber,
            String detail
    ) {
    }

    public record AuditEntry(
            Long id,
            IntegrityReviewAuditAction action,
            String actorUserId,
            String details,
            Instant createdAt
    ) {
    }
}
