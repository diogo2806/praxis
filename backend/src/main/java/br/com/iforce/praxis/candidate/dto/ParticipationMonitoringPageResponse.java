package br.com.iforce.praxis.candidate.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Página de participações individuais e por jornada da empresa.")
public record ParticipationMonitoringPageResponse(
        List<ParticipationMonitoringResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
