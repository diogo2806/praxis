package br.com.iforce.praxis.marketplace.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull Long orderId,
        @Min(1) @Max(5) short rating,
        @Size(max = 2000) String comment
) {
}
