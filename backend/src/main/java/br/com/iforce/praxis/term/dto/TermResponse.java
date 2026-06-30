package br.com.iforce.praxis.term.dto;

import io.swagger.v3.oas.annotations.media.Schema;


@Schema(description = "Termo exibido ao recrutador para aceite.")
public record TermResponse(
        String type,
        String version,
        String text
) {
}
