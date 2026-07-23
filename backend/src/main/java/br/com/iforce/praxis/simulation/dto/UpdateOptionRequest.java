package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.shared.model.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Min;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;


import java.util.Map;


@Schema(description = "Dados para atualizar parcialmente uma resposta.")
public record UpdateOptionRequest(
        @Size(max = 800)
        String text,

        Map<@NotBlank @Size(max = 140) String, @Min(0) @Max(100) Integer> competencyLevels,

        Boolean isBest,

        Boolean isCritical,

        @Size(max = 120)
        String nextNodeId,

        @Size(max = 1000)
        String resultingTone,

        @Size(max = 1500)
        @Schema(description = "Texto simplificado para leitores de tela. Envie string vazia para remover.", nullable = true)
        String plainTextDescription,

        @Size(max = 1000)
        @Schema(description = "URL pública de áudio descritivo da resposta. Envie string vazia para remover.", nullable = true)
        String audioDescriptionUrl,

        @Size(max = 1000)
        @Schema(description = "URL pública da mídia. Envie string vazia para remover a mídia.", nullable = true)
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
