package br.com.iforce.praxis.gupy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;


import java.math.BigDecimal;

import java.net.URI;


@Schema(description = "Pedido da Gupy para registrar um candidato em um teste Práxis.")
public record CreateCandidateRequest(
        @NotBlank
        @JsonProperty("company_id")
        @Schema(example = "empresa-123")
        String companyId,

        @NotBlank
        @JsonProperty("document_id")
        @Schema(example = "candidate-document-456")
        String documentId,

        @NotBlank
        @JsonProperty("test_id")
        @Schema(example = "sim-atendimento-n2")
        String testId,

        @NotBlank
        @JsonProperty("name")
        @Schema(example = "Candidato Teste")
        String candidateName,

        @Email
        @NotBlank
        @JsonProperty("email")
        @Schema(example = "candidato@example.com")
        String candidateEmail,

        @JsonProperty("job_id")
        @Schema(example = "100", description = "Identificador da vaga na Gupy.")
        Long jobId,

        @NotNull
        @JsonProperty("callback_url")
        @Schema(example = "https://cliente.gupy.io/candidates/return")
        URI callbackUrl,

        @JsonProperty("result_webhook_url")
        @Schema(example = "https://cliente.gupy.io/result-webhook")
        URI resultWebhookUrl,

        @JsonProperty("accommodation_time_multiplier")
        @Schema(example = "1.50", description = "Multiplicador de tempo para acomodacoes de acessibilidade.")
        BigDecimal accommodationTimeMultiplier,

        @JsonProperty("candidate_type")
        @Schema(example = "external", allowableValues = {"internal", "external"})
        String candidateType,

        @JsonProperty("previous_result")
        @Schema(example = "fail", allowableValues = {"pass", "fail", "none"})
        String previousResult
) {
}
