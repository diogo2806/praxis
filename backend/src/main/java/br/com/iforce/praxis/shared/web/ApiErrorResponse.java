package br.com.iforce.praxis.shared.web;

import com.fasterxml.jackson.annotation.JsonProperty;
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

        @Schema(example = "Dados inválidos.")
        String message,

        @Schema(example = "/test/candidate")
        String path,

        Map<String, String> fields
) {
    @JsonProperty("mensagem")
    public String mensagem() {
        return message;
    }

    @JsonProperty("acao_sugerida")
    public String acaoSugerida() {
        if (status == 404) {
            return "Verifique o link enviado ao candidato.";
        }
        if (status == 400) {
            return "Revise os dados informados e tente novamente.";
        }
        if (status == 409) {
            return "Atualize a pagina e confira a etapa atual.";
        }
        return "Tente novamente ou acione o suporte se o problema continuar.";
    }
}
