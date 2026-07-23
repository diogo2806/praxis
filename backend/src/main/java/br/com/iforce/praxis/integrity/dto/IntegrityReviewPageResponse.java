package br.com.iforce.praxis.integrity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Página da fila de revisão técnica.")
public record IntegrityReviewPageResponse(
        List<IntegrityReviewQueueItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
