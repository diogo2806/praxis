package br.com.iforce.praxis.gupy.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

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
        @Schema(example = "external", allowableValues = {"internal", "external"}, nullable = true)
        CandidateType candidateType,

        @JsonProperty("previous_result")
        @Schema(example = "fail", allowableValues = {"fail"}, nullable = true,
                description = "Resultado anterior. Use fail ou null quando não houver resultado anterior.")
        PreviousResult previousResult
) {
    public CreateCandidateRequest(
            String companyId,
            String documentId,
            String testId,
            String candidateName,
            String candidateEmail,
            URI resultWebhookUrl,
            BigDecimal accommodationTimeMultiplier,
            CandidateType candidateType,
            PreviousResult previousResult
    ) {
        this(
                companyId,
                documentId,
                testId,
                candidateName,
                candidateEmail,
                null,
                null,
                resultWebhookUrl,
                accommodationTimeMultiplier,
                candidateType,
                previousResult
        );
    }

    public enum CandidateType {
        INTERNAL("internal"),
        EXTERNAL("external");

        private final String value;

        CandidateType(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static CandidateType fromValue(String value) {
            if (value == null) {
                return null;
            }
            for (CandidateType candidateType : values()) {
                if (candidateType.value.equals(value)) {
                    return candidateType;
                }
            }
            throw new IllegalArgumentException("candidate_type deve ser internal ou external.");
        }
    }

    public enum PreviousResult {
        FAIL("fail");

        private final String value;

        PreviousResult(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static PreviousResult fromValue(String value) {
            if (value == null) {
                return null;
            }
            for (PreviousResult previousResult : values()) {
                if (previousResult.value.equals(value)) {
                    return previousResult;
                }
            }
            throw new IllegalArgumentException("previous_result deve ser fail ou null.");
        }
    }
}
