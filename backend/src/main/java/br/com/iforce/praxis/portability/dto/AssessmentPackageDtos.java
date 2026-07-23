package br.com.iforce.praxis.portability.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AssessmentPackageDtos {

    public static final String FORMAT_VERSION = "praxis-assessment-package/1.0";

    private AssessmentPackageDtos() {
    }

    public record PackageEnvelope(
            @NotBlank String formatVersion,
            @NotNull Instant exportedAt,
            @NotBlank String contentHash,
            @NotNull @Valid PackageManifest manifest
    ) {
    }

    public record PackageManifest(
            @NotNull @Valid PackageOrigin origin,
            @NotNull @Valid AssessmentContent assessment,
            @NotNull @Valid VersionContent version,
            @NotNull List<@Valid MediaAsset> mediaAssets
    ) {
    }

    public record PackageOrigin(
            @NotBlank String sourceSystem,
            @NotBlank String sourceAssessmentId,
            int sourceVersionNumber,
            @NotBlank String exportedBy,
            @NotNull Instant exportedAt
    ) {
    }

    public record AssessmentContent(
            @NotBlank @Size(max = 180) String name,
            @NotBlank @Size(max = 1000) String description,
            @Size(max = 1200) String criticalSituation,
            @Size(max = 120) String resultUse
    ) {
    }

    public record VersionContent(
            @NotBlank @Size(max = 120) String rootNodeId,
            @NotNull List<@Valid CompetencyContent> competencies,
            @NotNull List<@Valid NodeContent> nodes
    ) {
    }

    public record CompetencyContent(
            @NotBlank @Size(max = 180) String name,
            double weight,
            Double targetScore,
            String tier
    ) {
    }

    public record NodeContent(
            @NotBlank @Size(max = 120) String id,
            int turnIndex,
            @NotBlank @Size(max = 120) String speaker,
            @NotBlank @Size(max = 4000) String message,
            Integer timeLimitSeconds,
            String timeoutNextNodeId,
            boolean terminal,
            String reportText,
            Double positionX,
            Double positionY,
            String plainTextDescription,
            String audioDescriptionUrl,
            String mediaUrl,
            String mediaType,
            String mediaTranscript,
            String mediaCaptionsUrl,
            String mediaVersion,
            @NotNull List<@Valid OptionContent> options
    ) {
    }

    public record OptionContent(
            @NotBlank @Size(max = 120) String id,
            @NotBlank @Size(max = 2000) String text,
            String nextNodeId,
            boolean critical,
            String behavioralJustification,
            String plainTextDescription,
            String audioDescriptionUrl,
            String mediaUrl,
            String mediaType,
            String mediaTranscript,
            String mediaCaptionsUrl,
            String mediaVersion,
            @NotNull Map<@NotBlank String, Integer> competencyScores
    ) {
    }

    public record MediaAsset(
            @NotBlank String assetId,
            @NotBlank String url,
            @NotBlank String mediaType,
            long declaredSizeBytes,
            @NotBlank String sha256,
            @NotBlank String license,
            @NotBlank String origin,
            boolean embedded
    ) {
    }

    public record ValidationProblem(
            @NotBlank String path,
            @NotBlank String code,
            @NotBlank String message
    ) {
    }

    public record PackageValidationResponse(
            boolean valid,
            boolean importable,
            String calculatedHash,
            List<ValidationProblem> errors,
            List<ValidationProblem> warnings,
            List<String> competenciesRequiringConfirmation,
            Map<String, String> plannedIdMapping
    ) {
    }

    public record ImportPackageRequest(
            @NotNull @Valid PackageEnvelope packageEnvelope,
            @NotBlank @Size(max = 180) String newAssessmentName,
            boolean confirmed,
            boolean confirmCompetencies
    ) {
    }

    public record ImportPackageResponse(
            String simulationId,
            int versionNumber,
            String status,
            String sourceAssessmentId,
            int sourceVersionNumber,
            String packageHash,
            Map<String, String> idMapping,
            List<String> importedCompetencies
    ) {
    }
}
