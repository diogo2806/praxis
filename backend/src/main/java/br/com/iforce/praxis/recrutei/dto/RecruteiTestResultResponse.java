package br.com.iforce.praxis.recrutei.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;


import java.util.List;


/**
 * O resultado completo de uma prova, no formato esperado pela Recrutei.
 *
 * <p>Na visão do processo, é o "boletim" entregue à Recrutei depois que o
 * candidato termina: traz o nome e a descrição da prova, quem é o fornecedor
 * (Práxis), a situação atual, um resumo da pontuação, os links para abrir o
 * relatório (da empresa e do candidato) e a lista de competências avaliadas,
 * cada uma com sua nota e nível.</p>
 */
@Schema(description = "Resultado do teste Praxis no formato esperado pela Recrutei.")
public record RecruteiTestResultResponse(
        String title,

        @JsonProperty("test_id")
        String testId,

        String description,

        @JsonProperty("provider_name")
        String providerName,

        String status,

        @JsonProperty("result_summary")
        String resultSummary,

        @JsonProperty("result_url")
        String resultUrl,

        @JsonProperty("candidate_result_url")
        String candidateResultUrl,

        List<RecruteiTestResultItemResponse> competencies
) {
}
