package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;


@Schema(description = "Pedido de revisão humana feito pelo candidato (LGPD art. 20).")
public record ReviewRequest(
        @Size(max = 1000)
        @Schema(nullable = true, description = "Motivo opcional do pedido de revisão.")
        String reason
) {
}
