package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;

import br.com.iforce.praxis.simulation.dto.QuickStartTemplateSummaryResponse;

import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;

import br.com.iforce.praxis.simulation.model.QuickStartCategory;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;


import java.time.Instant;

import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;


class SimulationQuickStartServiceTest {

    private final SimulationQuickStartService service =
            new SimulationQuickStartService(null, null, null, new ObjectMapper());

    private final SimulationValidationService validationService = new SimulationValidationService(
            new PraxisProperties("http://localhost:8080", 168, 24, 70, 15, 0.001)
    );

    @Test
    void listsOneTemplatePerCategory() {
        List<QuickStartTemplateSummaryResponse> templates = service.listTemplates();

        assertThat(templates).hasSize(QuickStartCategory.values().length);
        assertThat(templates).allSatisfy(template -> {
            assertThat(template.category()).isNotNull();
            assertThat(template.title()).isNotBlank();
            assertThat(template.description()).isNotBlank();
            assertThat(template.nodeCount()).isPositive();
        });
    }

    @Test
    void everyTemplateGeneratesAPublishableVersion() {
        for (QuickStartCategory category : QuickStartCategory.values()) {
            SimulationVersionEntity version = service.buildDraftVersion(
                    service.templates().get(category),
                    Instant.now()
            );

            SimulationValidationResponse validation = validationService.validate(version);

            assertThat(validation.publishable())
                    .as("template %s deve gerar versão publicável (sem blockers): %s", category, validation.issues())
                    .isTrue();
            assertThat(validation.blockerCount()).isZero();
        }
    }
}
