package br.com.iforce.praxis.gupy.model;

import java.util.List;

public record PublishedSimulation(
        Long versionId,
        int versionNumber,
        String id,
        String name,
        String description,
        List<String> competencies,
        String rootNodeId,
        List<ScenarioNode> nodes
) {
}
