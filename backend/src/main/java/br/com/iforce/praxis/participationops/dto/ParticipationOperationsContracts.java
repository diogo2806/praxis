package br.com.iforce.praxis.participationops.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class ParticipationOperationsContracts {

    private ParticipationOperationsContracts() {
    }

    public record SavedViewRequest(
            @NotBlank @Size(max = 120) String name,
            boolean shared,
            @NotNull Map<String, Object> filters,
            Map<String, Object> sort,
            List<String> columns
    ) {
    }

    public record SavedViewResponse(
            String id,
            String ownerUserId,
            String name,
            boolean shared,
            Map<String, Object> filters,
            Map<String, Object> sort,
            List<String> columns,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
    }

    public record TagRequest(
            @NotBlank @Size(max = 80) String name,
            @NotBlank
            @Pattern(regexp = "#[0-9a-fA-F]{6}", message = "A cor deve usar o formato hexadecimal #RRGGBB.")
            String color,
            @Size(max = 500) String description
    ) {
    }

    public record TagResponse(
            String id,
            String name,
            String color,
            String description,
            String createdBy,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
    }

    public record ParticipationRef(
            @NotBlank
            @Pattern(regexp = "individual|journey", message = "Use individual ou journey.")
            String type,
            @NotBlank @Size(max = 120) String id
    ) {
    }

    public record BulkFilter(
            String simulationId,
            String candidate,
            String processStatus,
            String linkStatus,
            Boolean attention
    ) {
    }

    public record BulkRequest(
            @NotBlank
            @Pattern(
                    regexp = "RESEND|EXTEND|CANCEL|ADD_TAG|REMOVE_TAG|EXPORT",
                    message = "Ação de lote inválida."
            )
            String action,
            @NotBlank
            @Pattern(regexp = "EXPLICIT|FILTER", message = "Use EXPLICIT ou FILTER.")
            String selectionMode,
            @Valid List<ParticipationRef> selected,
            @Valid BulkFilter filter,
            @Min(1) @Max(365) Integer additionalDays,
            String tagId,
            @Size(max = 1000) String justification,
            @NotBlank @Size(max = 120) String idempotencyKey
    ) {
    }

    public record BulkPreviewRequest(
            @NotBlank
            @Pattern(
                    regexp = "RESEND|EXTEND|CANCEL|ADD_TAG|REMOVE_TAG|EXPORT",
                    message = "Ação de lote inválida."
            )
            String action,
            @NotBlank
            @Pattern(regexp = "EXPLICIT|FILTER", message = "Use EXPLICIT ou FILTER.")
            String selectionMode,
            @Valid List<ParticipationRef> selected,
            @Valid BulkFilter filter,
            @Min(1) @Max(365) Integer additionalDays,
            String tagId,
            @Size(max = 1000) String justification
    ) {
    }

    public record BulkExcludedItem(
            String participationType,
            String participationId,
            String reason
    ) {
    }

    public record BulkPreviewResponse(
            int selectedCount,
            int eligibleCount,
            int excludedCount,
            String action,
            String impact,
            List<BulkExcludedItem> excluded
    ) {
    }

    public record BulkItemResponse(
            String participationType,
            String participationId,
            String status,
            String reason,
            Instant processedAt
    ) {
    }

    public record BulkJobResponse(
            String id,
            String action,
            String selectionMode,
            String status,
            int totalItems,
            int processedItems,
            int succeededItems,
            int skippedItems,
            int failedItems,
            int progressPercent,
            String justification,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt,
            List<BulkItemResponse> items
    ) {
    }

    public record ParticipationTagsRequest(
            @NotEmpty @Size(max = 500) List<@Valid ParticipationRef> participations,
            @NotBlank String tagId
    ) {
    }

    @Schema(description = "Tags atribuídas a uma participação")
    public record ParticipationTagsResponse(
            String participationType,
            String participationId,
            List<TagResponse> tags
    ) {
    }
}
