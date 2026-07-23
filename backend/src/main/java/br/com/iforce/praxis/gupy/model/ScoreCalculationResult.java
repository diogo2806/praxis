package br.com.iforce.praxis.gupy.model;

import java.util.List;

public record ScoreCalculationResult(
        int score,
        int rawScore,
        int pathMaximumScore,
        int normalizedScore,
        String scoringAlgorithmVersion,
        List<ResultItem> resultItems,
        boolean humanReviewRequired,
        ReliabilityLevel reliabilityLevel,
        String auditTrail,
        ResultDecision decision
) {

    public ScoreCalculationResult(
            int score,
            List<ResultItem> resultItems,
            boolean humanReviewRequired,
            ReliabilityLevel reliabilityLevel,
            String auditTrail,
            ResultDecision decision
    ) {
        this(
                score,
                score,
                100,
                score,
                "legacy-path-normalized-v1",
                resultItems,
                humanReviewRequired,
                reliabilityLevel,
                auditTrail,
                decision
        );
    }
}
