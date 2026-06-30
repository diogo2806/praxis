package br.com.iforce.praxis.journey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Cria a tentativa de uma jornada para um candidato.")
public record CreateJourneyAttemptRequest(
        @NotBlank
        @Schema(example = "processo-trainee-2026-abc12345")
        String journeyId,

        @NotBlank
        @Size(max = 160)
        @Schema(example = "Maria Silva")
        String candidateName,

        @Email
        @NotBlank
        @Size(max = 180)
        @Schema(example = "maria@example.com")
        String candidateEmail,

        @Size(max = 80)
        @Schema(example = "principal", description = "Sequência da jornada que o candidato executará. Padrão: a primeira.")
        String sequenceKey
) {
}
