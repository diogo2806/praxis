package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.shared.model.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;


@Schema(description = "Dados para criar uma etapa do teste.")
public record CreateNodeRequest(
        @Size(max = 1200)
        @Schema(example = "Chegou quebrado. Quero meu dinheiro de volta agora.")
        String clientMessage,

        @Schema(example = "45")
        Integer timeLimitSeconds,

        @Size(max = 1000)
        @Schema(example = "Etapa inicial com pressao alta.")
        String timeJustification,

        @Size(max = 120)
        @Schema(description = "Próxima etapa usada quando o tempo da etapa esgota. Nulo encerra a trilha.", nullable = true)
        String timeoutNextNodeId,

        @Schema(description = "Marca a etapa como encerramento do fluxo.")
        boolean isFinal,

        @Size(max = 2000)
        @Schema(description = "Texto do relatorio enviado ao recrutador ao fim deste caminho.", nullable = true)
        String reportText,

        @Schema(description = "Posicao X do card no canvas.", nullable = true)
        Double positionX,

        @Schema(description = "Posicao Y do card no canvas.", nullable = true)
        Double positionY,

        @Size(max = 1500)
        @Schema(description = "Texto simplificado para leitores de tela.", nullable = true)
        String plainTextDescription,

        @Size(max = 1000)
        @Schema(description = "URL publica de audio descritivo da etapa.", nullable = true)
        String audioDescriptionUrl,

        @Size(max = 1000)
        @Schema(description = "URL pública da imagem ou áudio anexado à etapa.", nullable = true)
        String mediaUrl,

        @Schema(description = "Tipo da mídia anexada (IMAGE, AUDIO ou VIDEO).", nullable = true)
        MediaType mediaType,

        @Size(max = 8000)
        @Schema(description = "Transcrição textual acessível do áudio ou vídeo.", nullable = true)
        String mediaTranscript,

        @Size(max = 1000)
        @Schema(description = "URL pública da legenda WebVTT do vídeo.", nullable = true)
        String mediaCaptionsUrl,

        @Size(max = 120)
        @Schema(description = "Versão imutável do conteúdo multimídia apresentada ao candidato.", nullable = true)
        String mediaVersion
) {
}
