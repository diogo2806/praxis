package br.com.iforce.praxis.recrutei.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.net.URI;

@Schema(description = "Pedido da Recrutei para registrar um candidato em um teste Praxis.")
public record RecruteiCreateCandidateRequest(
        @NotBlank
        @JsonProperty("company_id")
        @Schema(example = "empresa-123")
        String companyId,

        @NotBlank
        @JsonProperty("candidate_id")
        @Schema(example = "candidate-456")
        String candidateId,

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

        @JsonProperty("vacancy_id")
        @Schema(example = "12345", description = "Identificador da vaga na Recrutei.")
        String vacancyId,

        @JsonProperty("result_webhook_url")
        @Schema(example = "https://api.recrutei.com.br/webhook/result")
        URI resultWebhookUrl,

        @JsonProperty("accommodation_time_multiplier")
        @Schema(example = "1.50", description = "Multiplicador de tempo para acomodações de acessibilidade.")
        BigDecimal accommodationTimeMultiplier
) {
}
