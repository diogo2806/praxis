package br.com.iforce.praxis.integrity.dto;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.integrity.model.IntegrityReviewDecision;
import br.com.iforce.praxis.integrity.model.IntegrityReviewStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Item neutro da fila de revisão técnica.")
public record IntegrityReviewQueueItemResponse(
        String attemptId,
        String candidateName,
        String candidateEmail,
        AttemptStatus attemptStatus,
        int alertCount,
        IntegrityReviewStatus reviewStatus,
        IntegrityReviewDecision decision,
        Instant updatedAt,
        Instant evidenceDiscardedAt
) {
}
