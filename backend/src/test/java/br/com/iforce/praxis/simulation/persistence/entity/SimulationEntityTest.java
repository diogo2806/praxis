package br.com.iforce.praxis.simulation.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationEntityTest {

    @Test
    void normalizesRecognizedLegacyPlanningDescription() {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setName("Analista de Suporte");
        simulation.setDescription("""
                Cargo: Analista de Suporte
                Competências: comunicação e resolução de problemas
                Situação crítica: cliente insatisfeito
                Uso do resultado: apoio à entrevista
                """);

        simulation.normalizeLegacyPlanningDescription();

        assertThat(simulation.getDescription())
                .isEqualTo("Avaliação situacional estruturada para Analista de Suporte.");
    }

    @Test
    void usesGenericDescriptionWhenLegacyPlanningHasNoSimulationName() {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setName("   ");
        simulation.setDescription("""
                Role: Support Analyst
                Competencies: communication
                Critical situation: customer complaint
                Result use: interview support
                """);

        simulation.normalizeLegacyPlanningDescription();

        assertThat(simulation.getDescription()).isEqualTo("Avaliação situacional estruturada.");
    }

    @Test
    void preservesFreeTextDescription() {
        SimulationEntity simulation = new SimulationEntity();
        String description = "O candidato deve orientar um cliente durante uma situação de pressão.";
        simulation.setName("Atendimento");
        simulation.setDescription(description);

        simulation.normalizeLegacyPlanningDescription();

        assertThat(simulation.getDescription()).isEqualTo(description);
    }

    @Test
    void preservesDescriptionWhenAnyLineUsesUnknownPlanningLabel() {
        SimulationEntity simulation = new SimulationEntity();
        String description = """
                Cargo: Analista
                Objetivo comercial: aumentar conversão
                Competências: negociação
                """;
        simulation.setName("Analista");
        simulation.setDescription(description);

        simulation.normalizeLegacyPlanningDescription();

        assertThat(simulation.getDescription()).isEqualTo(description);
    }

    @Test
    void preservesSingleRecognizedPlanningLine() {
        SimulationEntity simulation = new SimulationEntity();
        String description = "Cargo: Analista";
        simulation.setName("Analista");
        simulation.setDescription(description);

        simulation.normalizeLegacyPlanningDescription();

        assertThat(simulation.getDescription()).isEqualTo(description);
    }
}
