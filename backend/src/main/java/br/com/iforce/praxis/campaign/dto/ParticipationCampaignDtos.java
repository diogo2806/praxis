package br.com.iforce.praxis.campaign.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ParticipationCampaignDtos {

    private ParticipationCampaignDtos() {
    }

    public enum ReminderTargetState {
        NOT_OPENED,
        NOT_STARTED,
        IN_PROGRESS
    }

    public enum CommunicationEvent {
        DELIVERED,
        BOUNCED,
        OPENED
    }

    public record CsvPreviewRequest(
            @NotBlank @Size(max = 120) String simulationId,
            @NotBlank @Size(max = 120) String applicationCycleId,
            boolean consentRequired,
            boolean allowExistingActive,
            @NotBlank String csvContent
    ) {
    }

    public record CampaignParticipantInput(
            @Min(2) int rowNumber,
            @NotBlank @Size(max = 180) String name,
            @Email @NotBlank @Size(max = 320) String email,
            boolean consentConfirmed,
            @DecimalMin("1.00") @DecimalMax("3.00") BigDecimal accommodationMultiplier
    ) {
    }

    public record CsvRowDiagnostic(
            int rowNumber,
            String name,
            String email,
            boolean valid,
            List<String> errors,
            List<String> warnings
    ) {
    }

    public record CsvPreviewResponse(
            List<String> headers,
            int totalRows,
            int validRows,
            int invalidRows,
            int availableCapacity,
            boolean planLimitExceeded,
            List<CsvRowDiagnostic> rows,
            List<CampaignParticipantInput> validParticipants
    ) {
    }

    public record ReminderRuleInput(
            @Min(1) @Max(3) int reminderIndex,
            @Min(1) @Max(2160) int sendAfterHours,
            @NotNull ReminderTargetState targetState,
            @NotBlank @Size(max = 240) String subjectTemplate,
            @NotBlank @Size(max = 10000) String bodyTemplate
    ) {
    }

    public record CreateCampaignRequest(
            @NotBlank @Size(max = 180) String name,
            @NotBlank @Size(max = 120) String simulationId,
            @NotBlank @Size(max = 120) String applicationCycleId,
            @Size(max = 200) String applicationContext,
            @NotNull Instant initialSendAt,
            @Min(1) @Max(90) int linkValidityDays,
            boolean consentRequired,
            boolean allowExistingActive,
            @NotBlank @Size(max = 240) String subjectTemplate,
            @NotBlank @Size(max = 10000) String bodyTemplate,
            @Min(1) @Max(3650) int retentionDays,
            @NotEmpty List<@Valid CampaignParticipantInput> participants,
            @Size(max = 3) List<@Valid ReminderRuleInput> reminders,
            @NotBlank @Size(max = 120) String idempotencyKey
    ) {
    }

    public record MessagePreviewRequest(
            @NotBlank @Size(max = 240) String subjectTemplate,
            @NotBlank @Size(max = 10000) String bodyTemplate,
            @NotBlank @Size(max = 180) String sampleName,
            @NotBlank @Size(max = 180) String campaignName
    ) {
    }

    public record MessagePreviewResponse(String subject, String body, List<String> variables) {
    }

    public record CommunicationEventRequest(@NotNull CommunicationEvent event) {
    }

    public record CampaignParticipantResponse(
            UUID id,
            int rowNumber,
            String candidateName,
            String maskedEmail,
            boolean consentConfirmed,
            String attemptId,
            String candidateUrl,
            Instant linkExpiresAt,
            String participationStatus,
            String communicationStatus,
            String lastError,
            Instant openedAt,
            Instant startedAt,
            Instant completedAt,
            Instant expiredAt,
            Instant cancelledAt
    ) {
    }

    public record CampaignTotals(
            int total,
            int pending,
            int delivered,
            int failed,
            int bounced,
            int opened,
            int notStarted,
            int inProgress,
            int completed,
            int expired,
            int cancelled
    ) {
    }

    public record CampaignResponse(
            UUID id,
            String name,
            String simulationId,
            String applicationCycleId,
            String applicationContext,
            String status,
            Instant initialSendAt,
            int linkValidityDays,
            boolean consentRequired,
            boolean allowExistingActive,
            Instant retentionUntil,
            String createdBy,
            Instant createdAt,
            CampaignTotals totals,
            List<CampaignParticipantResponse> participants,
            List<Map<String, Object>> reminders
    ) {
    }
}
