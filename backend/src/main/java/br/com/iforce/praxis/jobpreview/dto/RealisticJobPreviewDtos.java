package br.com.iforce.praxis.jobpreview.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class RealisticJobPreviewDtos {

    private RealisticJobPreviewDtos() {
    }

    public enum ScopeType { JOB, ROLE, JOURNEY }
    public enum DisplayPosition { BEFORE, AFTER, BOTH }
    public enum DisplayStage { BEFORE, AFTER }
    public enum MediaType { IMAGE, AUDIO, VIDEO }

    public record MediaItem(
            @NotNull MediaType type,
            @NotBlank @Size(max = 1000) String url,
            @NotBlank @Size(max = 500) String alternativeText,
            @Size(max = 1000) String transcriptUrl
    ) {
    }

    public record PreviewContentRequest(
            @NotBlank @Size(max = 5000) String responsibilities,
            @NotBlank @Size(max = 5000) String autonomy,
            @NotBlank @Size(max = 5000) String pressureContext,
            @NotBlank @Size(max = 5000) String contactFrequency,
            @NotBlank @Size(max = 5000) String criticalSituations,
            @NotBlank @Size(max = 5000) String routineDescription,
            @NotBlank @Size(max = 5000) String workConditions,
            @NotBlank @Size(max = 5000) String positiveAspects,
            @NotBlank @Size(max = 10000) String alternativeText,
            List<@NotNull MediaItem> media,
            List<@NotBlank @Size(max = 120) String> scenarioNodeIds
    ) {
    }

    public record CreatePreviewRequest(
            @NotNull ScopeType scopeType,
            @NotBlank @Size(max = 160) String scopeKey,
            @NotBlank @Size(max = 200) String title,
            @NotNull DisplayPosition displayPosition,
            boolean acknowledgementRequired,
            @NotNull PreviewContentRequest content
    ) {
    }

    public record UpdatePreviewDraftRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull DisplayPosition displayPosition,
            boolean acknowledgementRequired,
            @NotNull PreviewContentRequest content
    ) {
    }

    public record PreviewSummaryResponse(
            String id,
            ScopeType scopeType,
            String scopeKey,
            String title,
            DisplayPosition displayPosition,
            boolean acknowledgementRequired,
            Integer activeVersionNumber,
            Integer draftVersionNumber,
            Instant updatedAt
    ) {
    }

    public record PreviewVersionResponse(
            String previewId,
            String versionId,
            int versionNumber,
            String status,
            String title,
            DisplayPosition displayPosition,
            boolean acknowledgementRequired,
            PreviewContentRequest content,
            Instant publishedAt
    ) {
    }

    public record CandidatePreviewResponse(
            String previewId,
            String versionId,
            int versionNumber,
            String title,
            DisplayStage displayStage,
            boolean acknowledgementRequired,
            PreviewContentRequest content,
            String informationalNotice
    ) {
    }

    public record CandidatePreviewReactionRequest(
            boolean acknowledged,
            boolean voluntaryWithdrawal,
            @Min(1) @Max(5) Integer clarityScore,
            @Min(1) @Max(5) Integer realismScore,
            @Min(1) @Max(5) Integer expectationCompatibilityScore
    ) {
    }

    public record PreviewMetricsResponse(
            String previewId,
            int versionNumber,
            long presentations,
            long acknowledgements,
            long voluntaryWithdrawals,
            double acknowledgementRatePercent,
            double withdrawalRatePercent,
            Double averageClarity,
            Double averageRealism,
            Double averageExpectationCompatibility,
            boolean sampleSuppressed,
            int minimumSample,
            String privacyNotice
    ) {
    }
}
