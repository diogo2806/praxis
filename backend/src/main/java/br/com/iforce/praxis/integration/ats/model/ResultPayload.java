package br.com.iforce.praxis.integration.ats.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Resultado normalizado da avaliação que será transformado para o formato de cada ATS.
 * Este é o modelo canônico que todos os adapters entendem.
 */
public record ResultPayload(
    @JsonProperty("candidate_id")
    String candidateId,

    @JsonProperty("simulation_id")
    String simulationId,

    @JsonProperty("attempt_id")
    String attemptId,

    @JsonProperty("score")
    int score,

    @JsonProperty("result_id")
    String resultId,

    @JsonProperty("competencies")
    List<CompetencyScore> competencies,

    @JsonProperty("decision")
    String decision,

    @JsonProperty("human_review_required")
    boolean humanReviewRequired,

    @JsonProperty("explanation")
    String explanation
) {

    public record CompetencyScore(
        @JsonProperty("name")
        String name,

        @JsonProperty("score")
        int score,

        @JsonProperty("level")
        String level
    ) {}
}
