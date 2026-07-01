package br.com.iforce.praxis.recrutei.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * Resposta devolvida à Recrutei após inscrever o candidato na prova.
 *
 * <p>Na visão do processo, é o "comprovante de inscrição": entrega o link para
 * o candidato fazer a prova, o identificador que a Recrutei usará depois para
 * consultar o resultado e o identificador da vaga, devolvido tal como veio para
 * a Recrutei conseguir relacionar tudo do lado dela.</p>
 */
@Schema(description = "Resposta com URL da simulação e identificador para consulta do resultado.")
public record RecruteiCreateCandidateResponse(
        @JsonProperty("test_url")
        @Schema(example = "http://localhost:8080/candidate/attempts/att_123")
        String testUrl,

        @JsonProperty("test_result_id")
        @Schema(example = "res_123")
        String testResultId,

        @JsonProperty("vacancy_id")
        @Schema(example = "12345", description = "Identificador da vaga na Recrutei, ecoado de volta.")
        String vacancyId
) {
}
