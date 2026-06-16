package br.com.iforce.praxis.gupy.model;

import java.util.List;

public record ScoreCalculationResult(
        int score,
        List<ResultItem> resultItems,
        boolean humanReviewRequired,
        String auditTrail,
        ResultDecision decision
) {
}
