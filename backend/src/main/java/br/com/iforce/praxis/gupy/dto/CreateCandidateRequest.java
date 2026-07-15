package br.com.iforce.praxis.gupy.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;

@Schema(description = "Pedido da Gupy para registrar um candidato em um teste Práxis.")
public final class CreateCandidateRequest {

    @NotNull
    @Positive
    @JsonProperty("company_id")
    @Schema(type = "integer", format = "int64", example = "1")
    private final Long companyId;

    @NotNull
    @Positive
    @JsonProperty("document_id")
    @Schema(type = "integer", format = "int64", example = "4398157034")
    private final Long documentId;

    @NotBlank
    @JsonProperty("test_id")
    @Schema(example = "sim-atendimento-n2")
    private final String testId;

    @NotBlank
    @JsonProperty("name")
    @Schema(example = "Candidato Teste")
    private final String candidateName;

    @Email
    @NotBlank
    @JsonProperty("email")
    @Schema(example = "candidato@example.com")
    private final String candidateEmail;

    @JsonProperty("job_id")
    @Schema(type = "integer", format = "int64", example = "100", description = "Identificador da vaga na Gupy.")
    private final Long jobId;

    @NotNull
    @JsonProperty("callback_url")
    @Schema(example = "https://cliente.gupy.io/candidates/return")
    private final URI callbackUrl;

    @JsonProperty("result_webhook_url")
    @Schema(example = "https://cliente.gupy.io/result-webhook")
    private final URI resultWebhookUrl;

    @JsonProperty("accommodation_time_multiplier")
    @Schema(example = "1.50", description = "Multiplicador de tempo para acomodacoes de acessibilidade.")
    private final BigDecimal accommodationTimeMultiplier;

    @JsonProperty("candidate_type")
    @Schema(example = "external", allowableValues = {"internal", "external"}, nullable = true)
    private final CandidateType candidateType;

    @JsonProperty("previous_result")
    @Schema(example = "fail", allowableValues = {"fail", "null"}, nullable = true)
    private final PreviousResult previousResult;

    @JsonIgnore
    private final String internalCompanyId;

    @JsonIgnore
    private final String internalDocumentId;

    @JsonCreator
    public CreateCandidateRequest(
            @JsonProperty("company_id")
            @JsonDeserialize(using = StrictInt64Deserializer.class)
            Long companyId,
            @JsonProperty("document_id")
            @JsonDeserialize(using = StrictInt64Deserializer.class)
            Long documentId,
            @JsonProperty("test_id") String testId,
            @JsonProperty("name") String candidateName,
            @JsonProperty("email") String candidateEmail,
            @JsonProperty("job_id") Long jobId,
            @JsonProperty("callback_url") URI callbackUrl,
            @JsonProperty("result_webhook_url") URI resultWebhookUrl,
            @JsonProperty("accommodation_time_multiplier") BigDecimal accommodationTimeMultiplier,
            @JsonProperty("candidate_type") CandidateType candidateType,
            @JsonProperty("previous_result") PreviousResult previousResult
    ) {
        this.companyId = companyId;
        this.documentId = documentId;
        this.testId = testId;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.jobId = jobId;
        this.callbackUrl = callbackUrl;
        this.resultWebhookUrl = resultWebhookUrl;
        this.accommodationTimeMultiplier = accommodationTimeMultiplier;
        this.candidateType = candidateType;
        this.previousResult = previousResult;
        this.internalCompanyId = companyId == null ? null : Long.toString(companyId);
        this.internalDocumentId = documentId == null ? null : Long.toString(documentId);
    }

    /**
     * Construtor interno mantido para provedores que reutilizam o fluxo de tentativa,
     * mas possuem identificadores textuais próprios. Ele não é usado na desserialização
     * do contrato público da Gupy.
     */
    public CreateCandidateRequest(
            String companyId,
            String documentId,
            String testId,
            String candidateName,
            String candidateEmail,
            URI resultWebhookUrl,
            BigDecimal accommodationTimeMultiplier,
            String candidateType,
            String previousResult
    ) {
        this.companyId = null;
        this.documentId = null;
        this.testId = testId;
        this.candidateName = candidateName;
        this.candidateEmail = candidateEmail;
        this.jobId = null;
        this.callbackUrl = null;
        this.resultWebhookUrl = resultWebhookUrl;
        this.accommodationTimeMultiplier = accommodationTimeMultiplier;
        this.candidateType = CandidateType.fromValue(candidateType);
        this.previousResult = PreviousResult.fromValue(previousResult);
        this.internalCompanyId = companyId;
        this.internalDocumentId = documentId;
    }

    @JsonIgnore
    public String companyId() {
        return internalCompanyId;
    }

    @JsonIgnore
    public String documentId() {
        return internalDocumentId;
    }

    public String testId() {
        return testId;
    }

    public String candidateName() {
        return candidateName;
    }

    public String candidateEmail() {
        return candidateEmail;
    }

    public Long jobId() {
        return jobId;
    }

    public URI callbackUrl() {
        return callbackUrl;
    }

    public URI resultWebhookUrl() {
        return resultWebhookUrl;
    }

    public BigDecimal accommodationTimeMultiplier() {
        return accommodationTimeMultiplier;
    }

    public CandidateType candidateType() {
        return candidateType;
    }

    public PreviousResult previousResult() {
        return previousResult;
    }

    public Long contractCompanyId() {
        return companyId;
    }

    public Long contractDocumentId() {
        return documentId;
    }

    public enum CandidateType {
        INTERNAL("internal"),
        EXTERNAL("external");

        private final String value;

        CandidateType(String value) {
            this.value = value;
        }

        @JsonCreator
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

        @JsonValue
        public String value() {
            return value;
        }
    }

    public enum PreviousResult {
        FAIL("fail");

        private final String value;

        PreviousResult(String value) {
            this.value = value;
        }

        @JsonCreator
        public static PreviousResult fromValue(String value) {
            if (value == null || "null".equals(value)) {
                return null;
            }
            if (FAIL.value.equals(value)) {
                return FAIL;
            }
            throw new IllegalArgumentException("previous_result deve ser fail ou null.");
        }

        @JsonValue
        public String value() {
            return value;
        }
    }

    public static final class StrictInt64Deserializer extends JsonDeserializer<Long> {

        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                return (Long) context.handleUnexpectedToken(Long.class, parser);
            }
            return parser.getLongValue();
        }
    }
}
