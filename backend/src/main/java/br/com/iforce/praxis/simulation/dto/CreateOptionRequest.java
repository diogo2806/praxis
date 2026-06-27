package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.shared.model.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "Dados para criar uma resposta em uma etapa.")
public record CreateOptionRequest(
        @NotBlank
        @Size(max = 800)
        String text,

        @NotNull
        @Schema(example = "{\"Empatia\": 86, \"Aderencia a politica\": 92}")
        Map<@NotBlank @Size(max = 140) String, @Min(0) @Max(100) Integer> competencyLevels,

        boolean isBest,

        boolean isCritical,

        @Size(max = 120)
        String nextNodeId,

        @Size(max = 1000)
        String resultingTone,

        @Size(max = 1500)
        @Schema(description = "Texto simplificado para leitores de tela.", nullable = true)
        String plainTextDescription,

        @Size(max = 1000)
        @Schema(description = "URL pública de áudio descritivo da resposta.", nullable = true)
        String audioDescriptionUrl,

        @Size(max = 1000)
        @Schema(description = "URL pública da imagem ou áudio anexado à resposta.", nullable = true)
        String mediaUrl,

        @Schema(description = "Tipo da mídia anexada (IMAGE ou AUDIO).", nullable = true)
        MediaType mediaType
) {
}
