package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GupyTestCatalogMapperTest {

    private final GupyTestCatalogMapper mapper = new GupyTestCatalogMapper();

    @Test
    void omitsCategoryAndLevelWhenPublishedSimulationHasNoRealMetadataSource() {
        PublishedSimulation simulation = new PublishedSimulation(
                10L,
                3,
                "sim-logica",
                "Teste de lógica",
                "Avaliação publicada",
                List.of("Raciocínio"),
                Map.of("Raciocínio", 1.0),
                Map.of(),
                "root",
                List.of()
        );

        var response = mapper.toResponse(simulation);

        assertThat(response.id()).isEqualTo("sim-logica");
        assertThat(response.name()).isEqualTo("Teste de lógica");
        assertThat(response.description()).isEqualTo("Avaliação publicada");
        assertThat(response.category()).isNull();
        assertThat(response.level()).isNull();
    }
}
