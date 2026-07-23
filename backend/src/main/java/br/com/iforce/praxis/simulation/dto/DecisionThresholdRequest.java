package br.com.iforce.praxis.simulation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record DecisionThresholdRequest(
        @Min(0) @Max(100) int score,
        @NotBlank @Size(max = 1000) String populationDescription,
        @NotBlank @Size(min = 20, max = 2000) String justification,
        @NotBlank @Size(min = 10, max = 2000) String evidence,
        @NotNull Instant validFrom,
        Instant validUntil,
        boolean approve
) {
}
