package br.com.iforce.praxis.localization.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CandidateContentLocalizationServiceOptionIdTest {

    @Test
    void generatesTheSamePublicLabelsUsedByTheCandidateFlow() {
        assertThat(CandidateContentLocalizationService.optionLabel(0)).isEqualTo("A");
        assertThat(CandidateContentLocalizationService.optionLabel(1)).isEqualTo("B");
        assertThat(CandidateContentLocalizationService.optionLabel(25)).isEqualTo("Z");
        assertThat(CandidateContentLocalizationService.optionLabel(26)).isEqualTo("AA");
    }

    @Test
    void registersInternalAndPublicKeysWithoutCollision() {
        List<String> keys = CandidateContentLocalizationService.optionKeys(
                "turno-1",
                "B",
                0
        );

        assertThat(keys)
                .hasSize(2)
                .containsExactly(
                        "internal\u0000turno-1\u0000B",
                        "public\u0000turno-1\u0000A"
                );
        assertThat(keys.get(0)).isNotEqualTo(keys.get(1));
    }

    @Test
    void rejectsNegativeDisplayIndex() {
        assertThatThrownBy(() -> CandidateContentLocalizationService.optionLabel(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("O índice da alternativa não pode ser negativo.");
    }
}
