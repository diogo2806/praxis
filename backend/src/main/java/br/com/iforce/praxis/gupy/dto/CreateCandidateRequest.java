package br.com.iforce.praxis.gupy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.net.URI;

@Schema(description = "Pedido da Gupy para registrar um candidato em um teste Práxis.")
public record CreateCandidateRequest(
        @NotBlank
        @Schema(example = "empresa-123")
        String companyId,

        @NotBlank
        @Schema(example = "candidate-document-456")
        String documentId,

        @NotBlank
        @Schema(example = "sim-atendimento-n2")
        String testId,

        @NotBlank
        @Schema(example = "Candidato Teste")
        String candidateName,

        @Email
        @NotBlank
        @Schema(example = "candidato@example.com")
        String candidateEmail,

        @Schema(example = "https://cliente.gupy.io/callback")
        URI callbackUrl,

        @Schema(example = "https://cliente.gupy.io/result-webhook")
        URI resultWebhookUrl,

        @Schema(example = "external", allowableValues = {"internal", "external"})
        String candidateType,

        @Schema(example = "fail", allowableValues = {"pass", "fail", "none"})
        String previousResult
) {
}
