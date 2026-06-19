package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.shared.model.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Alternativa visível ao candidato, sem gabarito, pesos ou marcadores internos.")
public record CandidateOptionResponse(
        @Schema(example = "opcao-equilibrada")
        String id,

        @Schema(example = "Acolho a frustração, peço os dados mínimos e explico o próximo passo.")
        String text,

        @Schema(description = "Texto alternativo simplificado para leitores de tela.", nullable = true)
        String plainTextDescription,

        @Schema(description = "URL publica de audio descritivo da alternativa.", nullable = true)
        String audioDescriptionUrl,

        @Schema(description = "URL pública da imagem ou áudio da alternativa.", nullable = true)
        String mediaUrl,

        @Schema(description = "Tipo da mídia (IMAGE ou AUDIO).", nullable = true)
        MediaType mediaType
) {
}
