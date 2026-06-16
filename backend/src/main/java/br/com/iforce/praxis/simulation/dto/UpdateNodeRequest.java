package br.com.iforce.praxis.simulation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para atualizar parcialmente um turno no grafo da simulacao.")
public record UpdateNodeRequest(
        @Size(max = 1200)
        String clientMessage,

        Integer timeLimitSeconds,

        @Size(max = 1000)
        String timeJustification
) {
}
