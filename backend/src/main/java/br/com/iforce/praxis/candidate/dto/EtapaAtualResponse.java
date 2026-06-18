package br.com.iforce.praxis.candidate.dto;

import br.com.iforce.praxis.shared.model.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Situacao atual da avaliacao vista pelo candidato.")
public record EtapaAtualResponse(
        @Schema(example = "1")
        int numero,

        @Schema(example = "Cliente")
        String pessoa,

        @Schema(example = "Preciso de ajuda com este atendimento.")
        String descricao,

        @Schema(example = "45", nullable = true)
        Integer tempoLimiteSegundos,

        @Schema(description = "URL publica da imagem ou audio da situacao.", nullable = true)
        String midiaUrl,

        @Schema(description = "Tipo da midia (IMAGE ou AUDIO).", nullable = true)
        MediaType tipoMidia,

        List<RespostaResponse> alternativas
) {
}
