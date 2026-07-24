package br.com.iforce.praxis.quality.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class AssessmentQualityDtos {

    private AssessmentQualityDtos() {
    }

    public enum CriterionType {
        NUMERIC,
        CATEGORY
    }

    public record ExternalCriterionRequest(
            @NotBlank String candidateAttemptId,
            @NotBlank @Size(max = 80) String criterionCode,
            @NotBlank @Size(max = 180) String criterionLabel,
            @NotNull CriterionType criterionType,
            BigDecimal numericValue,
            @Size(max = 180) String categoryValue,
            Instant observedAt
    ) {
    }

    public record ExternalCriterionResponse(
            String id,
            String candidateAttemptId,
            String criterionCode,
            String criterionLabel,
            CriterionType criterionType,
            BigDecimal numericValue,
            String categoryValue,
            Instant observedAt,
            Instant createdAt
    ) {
    }

    public record SensitiveAnalysisRequest(
            @NotBlank @Size(max = 80) String groupCriterionCode,
            @NotBlank @Size(max = 500) String purpose,
            @NotBlank @Size(max = 300) String legalBasis,
            @Min(10) @Max(500) Integer minimumSample
    ) {
    }

    public record QualityReportResponse(
            Instant generatedAt,
            QualityScope scope,
            ObservedSummary observed,
            List<ScoreBucket> scoreDistribution,
            List<AlternativeMetric> alternatives,
            List<PathMetric> paths,
            List<ScenarioMetric> scenarios,
            List<CompetencyEstimate> competencies,
            List<ExternalCriterionRelation> externalCriteria,
            SensitiveAnalysis sensitiveAnalysis,
            List<AnalyticalRecommendation> recommendations,
            List<String> warnings,
            Methodology methodology
    ) {
    }

    public record QualityScope(
            String simulationId,
            Integer simulationVersionNumber,
            Long gupyJobId,
            Instant from,
            Instant to
    ) {
    }

    public record ObservedSummary(
            long sampleSize,
            long completed,
            long abandonedOrExpired,
            double completionRatePercent,
            Double meanScore,
            Double standardDeviation,
            Integer percentile10,
            Integer percentile25,
            Integer median,
            Integer percentile75,
            Integer percentile90,
            Double averageDurationSeconds,
            long pauseEvents
    ) {
    }

    public record ScoreBucket(
            int fromInclusive,
            int toInclusive,
            long count,
            double percentage
    ) {
    }

    public record AlternativeMetric(
            String nodeId,
            String optionId,
            long selectedCount,
            double selectionPercent,
            Double averageFinalScore,
            Double discriminationDifference,
            String diagnostic,
            String evidenceType
    ) {
    }

    public record PathMetric(
            String pathFingerprint,
            List<String> nodeIds,
            long count,
            double frequencyPercent,
            Double averageScore,
            Double averageDurationSeconds,
            String evidenceType
    ) {
    }

    public record ScenarioMetric(
            String nodeId,
            long presentations,
            long answers,
            long timeouts,
            Double averageResponseSeconds,
            long videoPauseEvents,
            String evidenceType
    ) {
    }

    public record CompetencyEstimate(
            String competency,
            long sampleSize,
            Double mean,
            Double standardDeviation,
            Double standardError,
            Double confidenceLow95,
            Double confidenceHigh95,
            String precisionLevel,
            String evidenceType
    ) {
    }

    public record ExternalCriterionRelation(
            String criterionCode,
            String criterionLabel,
            CriterionType criterionType,
            long sampleSize,
            Double pearsonCorrelation,
            List<CategoryMean> categoryMeans,
            String interpretation,
            String evidenceType
    ) {
    }

    public record CategoryMean(
            String category,
            long sampleSize,
            Double averageScore,
            boolean suppressed
    ) {
    }

    public record SensitiveAnalysis(
            String groupCriterionCode,
            int minimumSample,
            List<CategoryMean> groups,
            int suppressedGroups,
            String purpose,
            String legalBasis,
            String auditId
    ) {
    }

    public record AnalyticalRecommendation(
            String code,
            String severity,
            String title,
            String detail,
            String evidenceType
    ) {
    }

    public record Methodology(
            int minimumGeneralSample,
            int minimumSensitiveGroupSample,
            String scoreScale,
            String percentileMethod,
            String precisionMethod,
            String discriminationMethod,
            List<String> limitations
    ) {
    }

    public record ReactionScoreRequest(
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal value
    ) {
    }
}
