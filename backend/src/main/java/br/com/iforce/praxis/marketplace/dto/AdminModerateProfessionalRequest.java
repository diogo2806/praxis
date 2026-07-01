package br.com.iforce.praxis.marketplace.dto;

import jakarta.validation.constraints.Size;

public record AdminModerateProfessionalRequest(
        @Size(max = 1000) String reason
) {
}
