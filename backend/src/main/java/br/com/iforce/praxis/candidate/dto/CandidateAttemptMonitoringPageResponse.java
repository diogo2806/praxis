package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Página de tentativas para o centro operacional.")
public record CandidateAttemptMonitoringPageResponse(
        List<CandidateAttemptMonitoringResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
