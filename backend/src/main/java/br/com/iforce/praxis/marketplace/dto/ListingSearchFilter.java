package br.com.iforce.praxis.marketplace.dto;

import br.com.iforce.praxis.marketplace.model.ListingCategory;

import java.math.BigDecimal;

public record ListingSearchFilter(
        ListingCategory category,
        Long minPriceCents,
        Long maxPriceCents,
        BigDecimal minRating,
        String text,
        int page,
        int size
) {
}
