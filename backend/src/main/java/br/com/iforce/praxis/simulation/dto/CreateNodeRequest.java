package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.shared.model.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para criar um turno no grafo da simulacao.")
public record CreateNodeRequest(
        @NotBlank
        @Size(max = 1200)
        @Schema(example = "Chegou quebrado. Quero meu dinheiro de volta agora.")
        String clientMessage,

        @Schema(example = "45")
        Integer timeLimitSeconds,

        @Size(max = 1000)
        @Schema(example = "Turno inicial com pressao alta.")
        String timeJustification,

        @Size(max = 120)
        @Schema(description = "Proximo no usado quando o tempo do turno esgota. Nulo encerra a trilha.", nullable = true)
        String timeoutNextNodeId,

        @Size(max = 1500)
        @Schema(description = "Texto simplificado para leitores de tela.", nullable = true)
        String plainTextDescription,

        @Size(max = 1000)
        @Schema(description = "URL publica de audio descritivo do turno.", nullable = true)
        String audioDescriptionUrl,

        @Size(max = 1000)
        @Schema(description = "URL pública da imagem ou áudio anexado ao turno.", nullable = true)
        String mediaUrl,

        @Schema(description = "Tipo da mídia anexada (IMAGE ou AUDIO).", nullable = true)
        MediaType mediaType
) {
}
