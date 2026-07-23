package br.com.iforce.praxis.integrity.dto;

import br.com.iforce.praxis.integrity.model.IntegrityReviewDecision;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Status técnico neutro aprovado para compartilhamento no relatório empresarial.")
public record IntegrityReviewSharedStatusResponse(
        IntegrityReviewDecision decision,
        Instant decidedAt
) {
}
