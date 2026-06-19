package br.com.iforce.praxis.gupy.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CandidateAttempt(
        String id,
        String resultId,
        String tenantId,
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
        ResultDecision decision,
        boolean humanReviewRequired,
        ReliabilityLevel reliabilityLevel,
        BigDecimal accommodationTimeMultiplier,
        String companyResultString,
        Instant createdAt,
        Instant startedAt,
        Instant finishedAt
) {
}
