package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.shared.model.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Resposta visível ao candidato, sem gabarito, pesos ou marcadores internos.")
public record RespostaResponse(
        @Schema(example = "A")
        String id,

        @Schema(example = "Acolho a frustracao, peco os dados minimos e explico o proximo passo.")
        String texto,

        @Schema(description = "Texto alternativo simplificado para leitores de tela.", nullable = true)
        String descricaoAcessivel,

        @Schema(description = "URL pública de áudio descritivo da resposta.", nullable = true)
        String audioDescricaoUrl,

        @Schema(description = "URL pública da imagem ou áudio da resposta.", nullable = true)
        String midiaUrl,

        @Schema(description = "Tipo da midia (IMAGE ou AUDIO).", nullable = true)
        MediaType tipoMidia
) {
}
