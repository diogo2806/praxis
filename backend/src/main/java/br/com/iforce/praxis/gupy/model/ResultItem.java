package br.com.iforce.praxis.gupy.model;

public record ResultItem(
        String name,
        int score,
        ResultTier tier
) {
}
