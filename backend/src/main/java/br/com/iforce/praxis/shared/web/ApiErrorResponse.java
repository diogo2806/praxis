package br.com.iforce.praxis.shared.web;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

@Schema(description = "Resposta padronizada de erro da API.")
public record ApiErrorResponse(
        @Schema(example = "2026-06-15T12:00:00Z")
        Instant timestamp,

        @Schema(example = "400")
        int status,

        @Schema(example = "Bad Request")
        String error,

        @Schema(example = "Dados invalidos.")
        String message,

        @Schema(example = "/test/candidate")
        String path,

        Map<String, String> fields
) {
}
