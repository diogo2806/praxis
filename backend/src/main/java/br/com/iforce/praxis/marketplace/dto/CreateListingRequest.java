package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.ListingCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateListingRequest(
        @NotBlank @Size(max = 120) String sourceSimulationId,
        @Min(1) int sourceVersionNumber,
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 4000) String description,
        @NotNull ListingCategory category,
        @Min(1) long priceCents,
        Set<Long> previewNodeIds
) {
}
