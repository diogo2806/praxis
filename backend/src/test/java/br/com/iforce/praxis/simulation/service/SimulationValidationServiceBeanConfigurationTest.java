package br.com.iforce.praxis.simulation.service;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimulationValidationServiceBeanConfigurationTest {

    @Test
    void exposesExactlyOnePrimaryValidationService() {
        List<Class<? extends SimulationValidationService>> serviceCandidates = List.of(
                SimulationValidationService.class,
                ComparableSimulationValidationService.class,
                ConsistentSimulationValidationService.class
        ).stream()
                .filter(type -> type.isAnnotationPresent(Service.class))
                .toList();

        assertThat(serviceCandidates)
                .filteredOn(type -> type.isAnnotationPresent(Primary.class))
                .containsExactly(ConsistentSimulationValidationService.class);
    }
}
