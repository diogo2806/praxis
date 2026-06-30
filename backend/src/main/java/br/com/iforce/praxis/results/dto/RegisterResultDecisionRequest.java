package br.com.iforce.praxis.results.dto;

import br.com.iforce.praxis.audit.model.HumanDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterResultDecisionRequest(
        @NotNull HumanDecision decision,
        @Size(max = 1000) String note
) {
}
