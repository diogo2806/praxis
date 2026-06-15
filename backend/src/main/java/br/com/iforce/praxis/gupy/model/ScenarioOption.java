package br.com.iforce.praxis.gupy.model;

import java.util.Map;

public record ScenarioOption(
        String id,
        String text,
        String nextNodeId,
        Map<String, Integer> competencyScores,
        boolean critical,
        String auditNote
) {
}
