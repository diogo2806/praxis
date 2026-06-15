package br.com.iforce.praxis.gupy.model;

import java.util.List;

public record ScenarioNode(
        String id,
        int turnIndex,
        String speaker,
        String message,
        Integer timeLimitSeconds,
        List<ScenarioOption> options
) {
}
