package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.simulation.dto.UpdateBlueprintRequest;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationMapperServiceTest {

    private final SimulationMapperService service = new SimulationMapperService();

    @Test
    void reusesExistingCompetencyRowsWhenNamesAreKept() {
        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setRootNodeId("turno-1");
        SimulationCompetencyEntity empatia = competency(version, "Empatia", 0.5, 70);
        SimulationCompetencyEntity resolucao = competency(version, "Resolução", 0.5, 70);
        version.getCompetencies().add(empatia);
        version.getCompetencies().add(resolucao);

        UpdateBlueprintRequest request = new UpdateBlueprintRequest(
                "turno-1",
                List.of(
                        new UpdateBlueprintRequest.CompetencyRequest("Empatia", 0.6, 80, ResultTier.MINOR),
                        new UpdateBlueprintRequest.CompetencyRequest("Resolução", 0.4, 75, ResultTier.MAJOR)
                ),
                null,
                null
        );

        service.applyBlueprintUpdate(version, request);

        // As mesmas instâncias devem ser reaproveitadas (sem DELETE+INSERT na mesma chave única).
        assertThat(version.getCompetencies()).containsExactlyInAnyOrder(empatia, resolucao);
        assertThat(empatia.getWeight()).isEqualTo(0.6);
        assertThat(empatia.getTargetScore()).isEqualTo(80);
        assertThat(empatia.getTier()).isEqualTo(ResultTier.MINOR);
        assertThat(resolucao.getWeight()).isEqualTo(0.4);
        assertThat(resolucao.getTargetScore()).isEqualTo(75);
        assertThat(resolucao.getTier()).isEqualTo(ResultTier.MAJOR);
    }

    @Test
    void matchesExistingCompetencyIgnoringCaseAndWhitespace() {
        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setRootNodeId("turno-1");
        SimulationCompetencyEntity empatia = competency(version, "Empatia", 1.0, 70);
        version.getCompetencies().add(empatia);

        UpdateBlueprintRequest request = new UpdateBlueprintRequest(
                "turno-1",
                List.of(new UpdateBlueprintRequest.CompetencyRequest("  empatia  ", 1.0, 90, ResultTier.MINOR)),
                null,
                null
        );

        service.applyBlueprintUpdate(version, request);

        assertThat(version.getCompetencies()).containsExactly(empatia);
        assertThat(empatia.getName()).isEqualTo("empatia");
        assertThat(empatia.getTargetScore()).isEqualTo(90);
        assertThat(empatia.getTier()).isEqualTo(ResultTier.MINOR);
    }

    @Test
    void removesCompetenciesAbsentFromRequestAndAddsNewOnes() {
        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setRootNodeId("turno-1");
        SimulationCompetencyEntity empatia = competency(version, "Empatia", 0.5, 70);
        SimulationCompetencyEntity resolucao = competency(version, "Resolução", 0.5, 70);
        version.getCompetencies().add(empatia);
        version.getCompetencies().add(resolucao);

        UpdateBlueprintRequest request = new UpdateBlueprintRequest(
                "turno-1",
                List.of(
                        new UpdateBlueprintRequest.CompetencyRequest("Empatia", 0.5, 70, ResultTier.MAJOR),
                        new UpdateBlueprintRequest.CompetencyRequest("Comunicação", 0.5, 70, ResultTier.MINOR)
                ),
                null,
                null
        );

        service.applyBlueprintUpdate(version, request);

        Map<String, SimulationCompetencyEntity> byName = version.getCompetencies().stream()
                .collect(Collectors.toMap(SimulationCompetencyEntity::getName, c -> c));
        assertThat(byName.keySet()).containsExactlyInAnyOrder("Empatia", "Comunicação");
        // Competência mantida preserva a instância original.
        assertThat(byName.get("Empatia")).isSameAs(empatia);
        // Competência removida não permanece na coleção.
        assertThat(version.getCompetencies()).doesNotContain(resolucao);
    }

    private SimulationCompetencyEntity competency(
            SimulationVersionEntity version,
            String name,
            double weight,
            int targetScore
    ) {
        SimulationCompetencyEntity entity = new SimulationCompetencyEntity();
        entity.setSimulationVersion(version);
        entity.setName(name);
        entity.setWeight(weight);
        entity.setTargetScore(targetScore);
        return entity;
    }
}
