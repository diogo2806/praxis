package br.com.iforce.praxis.simulation.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationEntityDescriptionTest {

    @Test
    void normalizesPortuguesePlanningFormIntoPublicDescription() {
        SimulationEntity simulation = simulation(
                "Analista de Atendimento",
                """
                Cargo: Analista de Atendimento
                Situação crítica: Cliente exige estorno fora da política
                Competências: Empatia, Comunicação
                Uso do resultado: Triagem
                """
        );

        simulation.normalizeLegacyPlanningDescription();

        assertThat(simulation.getDescription())
                .isEqualTo("Avaliação situacional estruturada para Analista de Atendimento.");
    }

    @Test
    void normalizesLocalizedPlanningForm() {
        SimulationEntity simulation = simulation(
                "Support Analyst",
                """
                Role: Support Analyst
                Critical situation: Customer requests an exception
                Competencies: Empathy, Communication
                Result use: Screening
                """
        );

        simulation.normalizeLegacyPlanningDescription();

        assertThat(simulation.getDescription())
                .isEqualTo("Avaliação situacional estruturada para Support Analyst.");
    }

    @Test
    void preservesFreeDescription() {
        SimulationEntity simulation = simulation(
                "Analista",
                "Avaliação criada para observar decisões em um contexto realista e auditável."
        );

        simulation.normalizeLegacyPlanningDescription();

        assertThat(simulation.getDescription())
                .isEqualTo("Avaliação criada para observar decisões em um contexto realista e auditável.");
    }

    @Test
    void preservesSingleLineLegacySummaryWithInformationNotYetStructured() {
        SimulationEntity simulation = simulation(
                "Analista",
                "Cargo: Analista - Senioridade: Pleno - Erro crítico: Compartilhar credenciais"
        );

        simulation.normalizeLegacyPlanningDescription();

        assertThat(simulation.getDescription())
                .isEqualTo("Cargo: Analista - Senioridade: Pleno - Erro crítico: Compartilhar credenciais");
    }

    private SimulationEntity simulation(String name, String description) {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setName(name);
        simulation.setDescription(description);
        return simulation;
    }
}
