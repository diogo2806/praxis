package br.com.iforce.praxis.gupy.model;

import java.time.Instant;

public record AttemptAnswer(
        String nodeId,
        String optionId,
        boolean timedOut,
        Instant answeredAt
) {

    public static AttemptAnswer answered(String nodeId, String optionId, Instant answeredAt) {
        return new AttemptAnswer(nodeId, optionId, false, answeredAt);
    }

    public static AttemptAnswer timedOut(String nodeId, Instant answeredAt) {
        return new AttemptAnswer(nodeId, null, true, answeredAt);
    }
}
