package br.com.iforce.praxis.marketplace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        Long listingId,
        Long threadId,
        @NotBlank @Size(max = 4000) String body
) {
}
