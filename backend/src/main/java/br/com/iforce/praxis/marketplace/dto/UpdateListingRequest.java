package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.ListingCategory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateListingRequest(
        @Size(max = 150) String title,
        @Size(max = 4000) String description,
        ListingCategory category,
        @Min(1) Long priceCents,
        Set<Long> previewNodeIds
) {
}
