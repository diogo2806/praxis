package br.com.iforce.praxis.dashboard.dto;

import br.com.iforce.praxis.shared.model.MediaType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Indicadores analíticos usados exclusivamente pelo dashboard da empresa.
 */
public record DashboardAnalyticsResponse(
        Instant generatedAt,
        int periodDays,
        ParticipationSummary participations,
        List<ActivityPoint> activity,
        List<MediaQualityComparison> mediaQualityComparisons
) {

    public record ParticipationSummary(
            long total,
            long started,
            long notStarted,
            long inProgress,
            long completed,
            long abandoned,
            long expired,
            double completionRatePercent,
            double dropOffRatePercent,
            Double averageScoreLast30Days
    ) {
    }

    public record ActivityPoint(
            LocalDate date,
            long created,
            long completed,
            long abandoned
    ) {
    }

    /**
     * Comparação observada para uma versão exata de mídia. Versões diferentes nunca são agregadas.
     */
    public record MediaQualityComparison(
            MediaType mediaType,
            String mediaVersion,
            long sampleSize,
            long completed,
            double completionRatePercent,
            Double averageDurationSeconds,
            List<ResponseDistribution> responseDistribution
    ) {
    }

    public record ResponseDistribution(
            String responseId,
            long count,
            double percentage
    ) {
    }
}
