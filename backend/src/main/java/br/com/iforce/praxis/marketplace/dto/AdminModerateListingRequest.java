package br.com.iforce.praxis.marketplace.dto;

import jakarta.validation.constraints.Size;

public record AdminModerateListingRequest(
        @Size(max = 1000) String reason
) {
}
