package br.com.iforce.praxis.localization.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class SimulationLocalizationDtos {

    private SimulationLocalizationDtos() {
    }

    public enum LocaleStatus {
        DRAFT,
        IN_REVIEW,
        APPROVED
    }

    public enum LocaleSource {
        INVITATION,
        ATS,
        CANDIDATE,
        BASE_FALLBACK
    }

    public record ConfigureLocalesRequest(
            @NotBlank @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String baseLocale,
            @NotEmpty List<@NotBlank @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String> enabledLocales
    ) {
    }

    public record LocaleSummaryResponse(
            String locale,
            boolean baseLocale,
            LocaleStatus status,
            int revision,
            long translatedNodes,
            long totalNodes,
            long translatedOptions,
            long totalOptions,
            long translatedCompetencies,
            long totalCompetencies,
            double completenessPercent,
            Instant updatedAt,
            String reviewedBy,
            String approvedBy
    ) {
    }

    public record NodeTranslation(
            @NotBlank @Size(max = 120) String nodeId,
            @NotBlank @Size(max = 120) String speaker,
            @NotBlank @Size(max = 1200) String message,
            @Size(max = 2000) String reportText,
            @Size(max = 1500) String plainTextDescription,
            @Size(max = 20000) String mediaTranscript
    ) {
    }

    public record OptionTranslation(
            @NotBlank @Size(max = 120) String nodeId,
            @NotBlank @Size(max = 120) String optionId,
            @NotBlank @Size(max = 800) String text,
            @Size(max = 1500) String plainTextDescription,
            @Size(max = 20000) String mediaTranscript
    ) {
    }

    public record CompetencyTranslation(
            @NotBlank @Size(max = 140) String competencyName,
            @NotBlank @Size(max = 180) String displayName,
            @NotBlank @Size(max = 10000) String reportText
    ) {
    }

    public record LocaleContentRequest(
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 1000) String description,
            @NotBlank @Size(max = 20000) String instructions,
            @NotBlank @Size(max = 20000) String reportIntroduction,
            @NotNull List<@Valid NodeTranslation> nodes,
            @NotNull List<@Valid OptionTranslation> options,
            @NotNull List<@Valid CompetencyTranslation> competencies
    ) {
    }

    public record LocaleContentResponse(
            long simulationVersionId,
            String locale,
            boolean baseLocale,
            LocaleStatus status,
            int revision,
            LocaleContentRequest content,
            List<String> validationErrors,
            List<String> validationWarnings
    ) {
    }

    public record LocaleSelectionRequest(
            @NotBlank @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String locale,
            @NotNull LocaleSource source
    ) {
    }

    public record LocaleSelectionResponse(
            String requestedLocale,
            String selectedLocale,
            LocaleSource source,
            boolean fallbackApplied,
            List<String> availableLocales
    ) {
    }

    public record ImportLocalePackageRequest(
            @NotBlank @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$") String locale,
            @NotNull @Valid LocaleContentRequest content,
            boolean replaceExisting
    ) {
    }

    public record ExportLocalePackageResponse(
            String schemaVersion,
            long simulationVersionId,
            String locale,
            boolean baseLocale,
            LocaleStatus status,
            int revision,
            LocaleContentRequest content,
            Instant exportedAt
    ) {
    }
}
