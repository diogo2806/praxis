package br.com.iforce.praxis.partner.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record UpdatePartnerCatalogRequest(
        @NotNull Set<String> simulationIds
) {
}
