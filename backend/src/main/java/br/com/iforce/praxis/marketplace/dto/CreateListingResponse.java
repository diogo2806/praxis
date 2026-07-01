package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.ListingStatus;

public record CreateListingResponse(
        Long id,
        ListingStatus status
) {
}
