package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.shared.model.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para atualizar parcialmente um turno no grafo da simulacao.")
public record UpdateNodeRequest(
        @Size(max = 1200)
        String clientMessage,

        Integer timeLimitSeconds,

        @Size(max = 1000)
        String timeJustification,

        @Size(max = 120)
        @Schema(description = "Proximo no usado quando o tempo do turno esgota. Envie string vazia para remover.", nullable = true)
        String timeoutNextNodeId,

        @Size(max = 1500)
        @Schema(description = "Texto simplificado para leitores de tela. Envie string vazia para remover.", nullable = true)
        String plainTextDescription,

        @Size(max = 1000)
        @Schema(description = "URL publica de audio descritivo do turno. Envie string vazia para remover.", nullable = true)
        String audioDescriptionUrl,

        @Size(max = 1000)
        @Schema(description = "URL pública da mídia. Envie string vazia para remover a mídia.", nullable = true)
        String mediaUrl,

        @Schema(description = "Tipo da mídia anexada (IMAGE ou AUDIO).", nullable = true)
        MediaType mediaType
) {
}
