package br.com.iforce.praxis.simulation.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record NormativeGroupRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 160) String jobTitle,
        @Size(max = 100) String seniority,
        Long gupyJobId,
        @NotBlank @Size(max = 1000) String populationDescription,
        @NotNull Instant periodStart,
        @NotNull Instant periodEnd,
        @Min(30) int minimumSample,
        @AssertTrue boolean pathCompatibilityConfirmed,
        boolean activate
) {
}
