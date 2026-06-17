package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.shared.model.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Turno atual da simulação visto pelo candidato.")
public record CandidateNodeResponse(
        @Schema(example = "turno-1")
        String id,

        @Schema(example = "1")
        int turnIndex,

        @Schema(example = "Cliente")
        String speaker,

        @Schema(example = "Preciso de ajuda com este atendimento.")
        String message,

        @Schema(example = "45", nullable = true)
        Integer timeLimitSeconds,

        @Schema(description = "URL pública da imagem ou áudio do turno.", nullable = true)
        String mediaUrl,

        @Schema(description = "Tipo da mídia (IMAGE ou AUDIO).", nullable = true)
        MediaType mediaType,

        List<CandidateOptionResponse> options
) {
}
