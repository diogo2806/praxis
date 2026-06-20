package br.com.iforce.praxis.simulation.dto;

import br.com.iforce.praxis.shared.model.MediaType;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;

import java.util.List;
import java.util.Map;

public record SimulationVersionDetailResponse(
        String simulationId,
        String name,
        String description,
        String criticalSituation,
        String resultUse,
        int versionNumber,
        SimulationVersionStatus status,
        BlueprintDto blueprint,
        List<NodeDto> nodes
) {
    public record BlueprintDto(
            String rootNodeId,
            List<CompetencyWeightDto> competencies
    ) {
    }

    public record NodeDto(
            String id,
            int turnIndex,
            String speaker,
            String clientMessage,
            Integer timeLimitSeconds,
            String timeoutNextNodeId,
            boolean isFinal,
            String reportText,
            Double positionX,
            Double positionY,
            String plainTextDescription,
            String audioDescriptionUrl,
            String mediaUrl,
            MediaType mediaType,
            List<OptionDto> options
    ) {
    }

    public record OptionDto(
            String id,
            String text,
            Map<String, Integer> competencyLevels,
            boolean isCritical,
            String nextNodeId,
            String auditNote,
            String plainTextDescription,
            String audioDescriptionUrl,
            String mediaUrl,
            MediaType mediaType
    ) {
    }
}
