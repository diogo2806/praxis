package br.com.iforce.praxis.media.dto;

import br.com.iforce.praxis.shared.model.MediaType;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Resultado do upload de uma mídia (imagem ou áudio) para o cadastro de testes.")
public record MediaUploadResponse(
        @Schema(example = "https://cdn.exemplo.com/praxis-media/media/8c1c....png")
        String url,

        MediaType mediaType,

        @Schema(example = "image/png")
        String contentType,

        @Schema(example = "248192")
        long sizeBytes
) {
}
