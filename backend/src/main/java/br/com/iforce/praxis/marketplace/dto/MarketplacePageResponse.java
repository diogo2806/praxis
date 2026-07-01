package br.com.iforce.praxis.marketplace.dto;

import java.util.List;

public record MarketplacePageResponse<T>(
        List<T> content,
        int page,
        int totalPages,
        long totalElements
) {
}
