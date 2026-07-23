package br.com.iforce.praxis.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class AssessmentTemplateDtos {

    private AssessmentTemplateDtos() {
    }

    public enum TemplateScope {
        INTERNAL,
        SHARED,
        OFFICIAL
    }

    public enum TemplateStatus {
        DRAFT,
        IN_REVIEW,
        APPROVED,
        REJECTED,
        ARCHIVED
    }

    public record CreateTemplateRequest(
            @NotBlank @Size(max = 120) String sourceSimulationId,
            @Min(1) int sourceVersionNumber,
            @NotNull TemplateScope scope,
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 1200) String summary,
            @NotBlank @Size(max = 180) String jobRole,
            @NotBlank @Size(max = 180) String businessArea,
            @NotBlank @Size(max = 80) String seniority,
            @NotBlank @Size(max = 180) String sector,
            @Min(1) @Max(480) int durationMinutes,
            @NotBlank @Size(max = 16) String languageCode,
            @NotBlank @Size(max = 40) String complexity,
            @NotBlank String methodologyEvidence,
            @NotBlank String usageLimitations,
            @NotEmpty List<@NotBlank @Size(max = 140) String> competencies
    ) {
    }

    public record ReviewTemplateRequest(
            @NotNull TemplateStatus decision,
            @Size(max = 2000) String reviewNote
    ) {
    }

    public record InstantiateTemplateRequest(
            @NotBlank @Size(max = 180) String newAssessmentName
    ) {
    }

    public record TemplateSearch(
            String query,
            String jobRole,
            String businessArea,
            String seniority,
            String sector,
            String competency,
            String languageCode,
            String complexity,
            Boolean favoriteOnly
    ) {
    }

    public record TemplatePreviewResponse(
            int scenarioCount,
            int terminalCount,
            int optionCount,
            int durationMinutes,
            List<String> competencyCoverage,
            List<String> accessibilityRequirements,
            String rootNodeId
    ) {
    }

    public record TemplateResponse(
            UUID id,
            String ownerEmpresaId,
            String sourceEmpresaId,
            String sourceSimulationId,
            int sourceVersionNumber,
            int templateVersion,
            TemplateScope scope,
            TemplateStatus status,
            String title,
            String summary,
            String jobRole,
            String businessArea,
            String seniority,
            String sector,
            int durationMinutes,
            String languageCode,
            String complexity,
            String methodologyEvidence,
            String usageLimitations,
            String authorUserId,
            String reviewedBy,
            String reviewNote,
            Instant reviewedAt,
            Instant publishedAt,
            boolean favorite,
            TemplatePreviewResponse preview
    ) {
    }

    public record InstantiateTemplateResponse(
            UUID templateId,
            int templateVersion,
            String simulationId,
            int versionNumber,
            String status,
            String sourceSimulationId,
            int sourceVersionNumber
    ) {
    }
}
