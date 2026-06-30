package br.com.iforce.praxis.results.dto;

import java.util.List;


public record ResultsPageResponse(
        List<ResultListItemResponse> items,
        ResultsSummaryResponse summary,
        int page,
        int size,
        long totalItems,
        int totalPages
) {
}
