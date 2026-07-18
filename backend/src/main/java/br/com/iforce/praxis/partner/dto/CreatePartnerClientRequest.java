package br.com.iforce.praxis.partner.dto;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePartnerClientRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Size(max = 120) String externalCompanyId,
        @NotNull IntegrationProvider provider
) {
}
