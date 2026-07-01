package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.ProfessionalVerificationStatus;

public record RegisterProfessionalResponse(
        Long id,
        ProfessionalVerificationStatus status,
        String email
) {
}
