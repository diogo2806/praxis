package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.shared.model.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;


@Schema(description = "Dados para atualizar parcialmente uma etapa do teste.")
public record UpdateNodeRequest(
        @Size(max = 1200)
        String clientMessage,

        Integer timeLimitSeconds,

        @Size(max = 1000)
        String timeJustification,

        @Size(max = 120)
        @Schema(description = "Próxima etapa usada quando o tempo da etapa esgota. Envie string vazia para remover.", nullable = true)
        String timeoutNextNodeId,

        @Schema(description = "Marca ou desmarca a etapa como encerramento.", nullable = true)
        Boolean isFinal,

        @Size(max = 2000)
        @Schema(description = "Texto do relatorio. Envie string vazia para remover.", nullable = true)
        String reportText,

        @Schema(description = "Posicao X do card no canvas.", nullable = true)
        Double positionX,

        @Schema(description = "Posicao Y do card no canvas.", nullable = true)
        Double positionY,

        @Size(max = 1500)
        @Schema(description = "Texto simplificado para leitores de tela. Envie string vazia para remover.", nullable = true)
        String plainTextDescription,

        @Size(max = 1000)
        @Schema(description = "URL publica de audio descritivo da etapa. Envie string vazia para remover.", nullable = true)
        String audioDescriptionUrl,

        @Size(max = 1000)
        @Schema(description = "URL pública da mídia. Envie string vazia para remover a mídia.", nullable = true)
        String mediaUrl,

        @Schema(description = "Tipo da mídia anexada (IMAGE ou AUDIO).", nullable = true)
        MediaType mediaType
) {
}
