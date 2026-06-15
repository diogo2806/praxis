package br.com.iforce.praxis.gupy.model;

import java.time.Instant;

public record AttemptAnswer(
        String nodeId,
        String optionId,
        Instant answeredAt
) {
}
