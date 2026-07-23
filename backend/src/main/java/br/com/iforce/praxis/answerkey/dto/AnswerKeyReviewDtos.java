package br.com.iforce.praxis.answerkey.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

public final class AnswerKeyReviewDtos {

    private AnswerKeyReviewDtos() {
    }

    public record CreateRoundRequest(
            @Min(2) @Max(20) Integer minimumExperts,
            @DecimalMin("0.50") @DecimalMax("1.00") BigDecimal minimumConsensus
    ) {
    }

    public record AssignmentRequest(
            @NotBlank @Size(max = 180) String userId,
            @NotNull AssignmentRole role
    ) {
    }

    public enum AssignmentRole {
        EXPERT,
        APPROVER
    }

    public record EvidenceRequest(
            @NotBlank @Size(max = 1200) String task,
            @NotBlank @Size(max = 1200) String risk,
            @NotBlank @Size(max = 180) String competency,
            @NotBlank @Size(max = 600) String indicator,
            @NotNull @DecimalMin(value = "0.0001") BigDecimal weight
    ) {
    }

    public record OptionReviewRequest(
            @Min(0) @Max(100) int effectivenessScore,
            @NotBlank @Size(max = 4000) String behavioralJustification,
            @NotEmpty Map<@NotBlank String, @Min(0) @Max(100) Integer> competencyScores
    ) {
    }

    public record RoundResponse(
            UUID id,
            String simulationId,
            int versionNumber,
            int roundNumber,
            String status,
            int minimumExperts,
            BigDecimal minimumConsensus,
            String createdBy,
            Instant createdAt,
            String approvedBy,
            Instant approvedAt
    ) {
    }

    public record AssignmentResponse(
            String userId,
            AssignmentRole role,
            String status,
            Instant invitedAt,
            Instant submittedAt
    ) {
    }

    public record EvidenceResponse(
            String nodeId,
            String task,
            String risk,
            String competency,
            String indicator,
            BigDecimal weight,
            String updatedBy,
            Instant updatedAt
    ) {
    }

    public record OptionConsensusResponse(
            String nodeId,
            String optionId,
            String optionText,
            int reviewCount,
            BigDecimal averageScore,
            BigDecimal dispersion,
            BigDecimal consensus,
            String status,
            List<String> behavioralJustifications
    ) {
    }

    public record ReviewEventResponse(
            String eventType,
            String actorUserId,
            String eventDataJson,
            Instant occurredAt
    ) {
    }

    public record ReviewSummaryResponse(
            RoundResponse round,
            boolean approvable,
            int expectedOptions,
            int reviewedOptions,
            int submittedExperts,
            List<String> blockers,
            List<String> warnings,
            List<AssignmentResponse> assignments,
            List<EvidenceResponse> evidence,
            List<OptionConsensusResponse> options,
            List<ReviewEventResponse> history
    ) {
    }
}
