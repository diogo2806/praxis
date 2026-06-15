package br.com.iforce.praxis.simulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectSimulationVersionRequest(
        @NotBlank
        @Size(max = 1000)
        String reason
) {
}
