package br.com.iforce.praxis.gupy.model;

import java.util.List;

public record PublishedSimulation(
        String id,
        String name,
        String description,
        List<String> competencies,
        String rootNodeId,
        List<ScenarioNode> nodes
) {
}
