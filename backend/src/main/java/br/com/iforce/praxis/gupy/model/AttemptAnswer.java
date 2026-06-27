package br.com.iforce.praxis.gupy.model;

import java.time.Instant;

public record AttemptAnswer(
        String nodeId,
        String optionId,
        boolean timedOut,
        Instant answeredAt,
        Instant receivedAt
) {

    public static AttemptAnswer answered(String nodeId, String optionId, Instant answeredAt) {
        return answered(nodeId, optionId, answeredAt, answeredAt);
    }

    public static AttemptAnswer answered(String nodeId, String optionId, Instant answeredAt, Instant receivedAt) {
        return new AttemptAnswer(nodeId, optionId, false, answeredAt, receivedAt);
    }

    public static AttemptAnswer timedOut(String nodeId, Instant answeredAt) {
        return timedOut(nodeId, answeredAt, answeredAt);
    }

    public static AttemptAnswer timedOut(String nodeId, Instant answeredAt, Instant receivedAt) {
        return new AttemptAnswer(nodeId, null, true, answeredAt, receivedAt);
    }
}
