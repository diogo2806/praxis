package br.com.iforce.praxis.localization;

import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleContentRequest;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.NodeTranslation;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.OptionTranslation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationLocalizationContractTest {

    @Test
    void translationContractCannotChangeGraphScoringOrDestination() {
        Set<String> contentFields = componentNames(LocaleContentRequest.class);
        Set<String> nodeFields = componentNames(NodeTranslation.class);
        Set<String> optionFields = componentNames(OptionTranslation.class);

        assertThat(contentFields).containsExactlyInAnyOrder(
                "title",
                "description",
                "instructions",
                "reportintroduction",
                "nodes",
                "options",
                "competencies"
        );
        assertThat(nodeFields).doesNotContain(
                "weight",
                "destinationnodeid",
                "scoringformula",
                "displayorder"
        );
        assertThat(optionFields).doesNotContain(
                "weight",
                "destinationnodeid",
                "score",
                "displayorder"
        );
    }

    @Test
    void migrationPersistsLocaleOnAttemptAndKeepsTranslationsSeparated() throws IOException {
        String migration;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(
                "db/migration/V1110__create_simulation_locale_versions.sql")) {
            assertThat(input).isNotNull();
            migration = new String(input.readAllBytes(), StandardCharsets.UTF_8).toLowerCase();
        }

        assertThat(migration)
                .contains("simulation_version_locales")
                .contains("simulation_node_translations")
                .contains("simulation_option_translations")
                .contains("selected_locale varchar(16)")
                .contains("locale_source varchar(24)")
                .doesNotContain("destination_node_id")
                .doesNotContain("weight numeric")
                .doesNotContain("scoring_formula");
    }

    private Set<String> componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(RecordComponent::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
