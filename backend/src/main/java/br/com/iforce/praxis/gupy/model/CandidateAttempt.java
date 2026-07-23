package br.com.iforce.praxis.gupy.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Builder(toBuilder = true)
public record CandidateAttempt(
        String id,
        String resultId,
        String empresaId,
        String companyId,
        String simulationId,
        Long simulationVersionId,
        Integer simulationVersionNumber,
        String idempotencyKey,
        String candidateName,
        String candidateEmail,
        AttemptStatus status,
        Integer score,
        Integer rawScore,
        Integer pathMaximumScore,
        Integer normalizedScore,
        String scoringAlgorithmVersion,
        List<ResultItem> results,
        Map<String, AttemptAnswer> answersByNodeId,
        Map<String, Instant> servedAtByNodeId,
        ResultDecision decision,
        boolean humanReviewRequired,
        ReliabilityLevel reliabilityLevel,
        BigDecimal accommodationTimeMultiplier,
        String companyResultString,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {

    public CandidateAttempt(
            String id,
            String resultId,
            String empresaId,
            String companyId,
            String simulationId,
            Long simulationVersionId,
            Integer simulationVersionNumber,
            String idempotencyKey,
            String candidateName,
            String candidateEmail,
            AttemptStatus status,
            Integer score,
            List<ResultItem> results,
            Map<String, AttemptAnswer> answersByNodeId,
            Map<String, Instant> servedAtByNodeId,
            ResultDecision decision,
            boolean humanReviewRequired,
            ReliabilityLevel reliabilityLevel,
            BigDecimal accommodationTimeMultiplier,
            String companyResultString,
            Instant createdAt,
            Instant startedAt,
            Instant finishedAt
    ) {
        this(
                id,
                resultId,
                empresaId,
                companyId,
                simulationId,
                simulationVersionId,
                simulationVersionNumber,
                idempotencyKey,
                candidateName,
                candidateEmail,
                status,
                score,
                null,
                null,
                score,
                score == null ? null : "legacy-path-normalized-v1",
                results,
                answersByNodeId,
                servedAtByNodeId,
                decision,
                humanReviewRequired,
                reliabilityLevel,
                accommodationTimeMultiplier,
                companyResultString,
                createdAt,
                startedAt,
                finishedAt
        );
    }
}
