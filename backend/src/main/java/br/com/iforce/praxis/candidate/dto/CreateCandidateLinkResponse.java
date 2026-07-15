package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Link para o candidato acessar a simulacao e efeito da operação solicitada.")
public record CreateCandidateLinkResponse(
        @Schema(example = "att_abc123def456")
        String attemptId,

        @Schema(example = "https://praxis.example.com/candidato/att_abc123def456")
        String candidateUrl,

        @Schema(example = "Simulacao Atendimento N2")
        String simulationName,

        @Schema(example = "false", description = "Indica que a tentativa já existia e foi reaproveitada sem criar uma nova aplicação.")
        boolean reused,

        @Schema(
                example = "CREATED_NEW_APPLICATION",
                allowableValues = {
                        "CREATED_NEW_APPLICATION",
                        "REUSED_IDEMPOTENT_REQUEST",
                        "RESENT_EXISTING_LINK"
                },
                description = "Efeito efetivo da operação sobre a tentativa."
        )
        String operation
) {

    /** Compatibilidade para fluxos internos que ainda não expõem o efeito da operação. */
    public CreateCandidateLinkResponse(String attemptId, String candidateUrl, String simulationName) {
        this(attemptId, candidateUrl, simulationName, false, "CREATED_NEW_APPLICATION");
    }
}
