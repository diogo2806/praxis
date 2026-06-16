package br.com.iforce.praxis.gupy.model;

import java.util.List;
import java.util.Map;

public record PublishedSimulation(
        Long versionId,
        int versionNumber,
        String id,
        String name,
        String description,
        List<String> competencies,
        Map<String, Double> competencyWeights,
        String rootNodeId,
        List<ScenarioNode> nodes
) {
}
