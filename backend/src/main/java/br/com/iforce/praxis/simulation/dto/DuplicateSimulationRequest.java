package br.com.iforce.praxis.simulation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nome da nova avaliação criada a partir de uma versão existente. */
public record DuplicateSimulationRequest(
        @NotBlank(message = "Informe o nome da nova avaliação.")
        @Size(max = 180, message = "O nome deve ter no máximo 180 caracteres.")
        String name
) {
}
