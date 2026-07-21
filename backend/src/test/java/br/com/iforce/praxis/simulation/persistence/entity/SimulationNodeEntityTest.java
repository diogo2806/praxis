package br.com.iforce.praxis.simulation.persistence.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationNodeEntityTest {

    @Test
    void convertsZeroTimeLimitToNoLimit() {
        SimulationNodeEntity node = new SimulationNodeEntity();
        node.setTimeLimitSeconds(30);

        node.setTimeLimitSeconds(0);

        assertThat(node.getTimeLimitSeconds()).isNull();
    }

    @Test
    void keepsPositiveTimeLimit() {
        SimulationNodeEntity node = new SimulationNodeEntity();

        node.setTimeLimitSeconds(45);

        assertThat(node.getTimeLimitSeconds()).isEqualTo(45);
    }
}
