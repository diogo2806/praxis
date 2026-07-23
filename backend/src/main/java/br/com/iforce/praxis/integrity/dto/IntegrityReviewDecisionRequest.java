package br.com.iforce.praxis.integrity.dto;

import br.com.iforce.praxis.integrity.model.IntegrityReviewDecision;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Parecer humano neutro para uma revisão técnica.")
public record IntegrityReviewDecisionRequest(
        @NotNull IntegrityReviewDecision decision,
        @NotBlank @Size(max = 2000) String justification,
        boolean shareWithCompany
) {
}
