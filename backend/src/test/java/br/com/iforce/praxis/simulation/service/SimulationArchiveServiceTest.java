package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SimulationArchiveServiceTest {

    private final CurrentEmpresaService currentEmpresaService = mock(CurrentEmpresaService.class);
    private final SimulationRepository simulationRepository = mock(SimulationRepository.class);
    private final AuditEventService auditEventService = mock(AuditEventService.class);
    private final SimulationArchiveService service = new SimulationArchiveService(
            currentEmpresaService,
            simulationRepository,
            auditEventService
    );

    @Test
    void rejectsSimulationFromAnotherEmpresaOrMissingSimulation() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(simulationRepository.findByEmpresaIdAndId("empresa-1", "simulation-404"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.archiveSimulation("simulation-404"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(exception.getReason()).isEqualTo("Não encontramos este teste.");
                });

        verify(simulationRepository, never()).save(any(SimulationEntity.class));
        verifyNoInteractions(auditEventService);
    }

    @Test
    void archivesOnlyActiveVersionsAndAuditsLatestVersion() {
        SimulationVersionEntity draft = version(1, SimulationVersionStatus.DRAFT);
        SimulationVersionEntity alreadyArchived = version(2, SimulationVersionStatus.ARCHIVED);
        SimulationVersionEntity published = version(4, SimulationVersionStatus.PUBLISHED);
        SimulationEntity simulation = simulation("simulation-1", draft, alreadyArchived, published);

        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(simulationRepository.findByEmpresaIdAndId("empresa-1", "simulation-1"))
                .thenReturn(Optional.of(simulation));

        service.archiveSimulation("simulation-1");

        assertThat(draft.getStatus()).isEqualTo(SimulationVersionStatus.ARCHIVED);
        assertThat(alreadyArchived.getStatus()).isEqualTo(SimulationVersionStatus.ARCHIVED);
        assertThat(published.getStatus()).isEqualTo(SimulationVersionStatus.ARCHIVED);
        verify(simulationRepository).save(simulation);
        verify(auditEventService).appendSimulationVersionEvent(
                "empresa-1",
                "simulation-1",
                4,
                AuditEventType.SIMULATION_ARCHIVED,
                "Avaliação arquivada sem exclusão de histórico.",
                "{\"status\":\"archived\",\"archivedVersions\":2}"
        );
    }

    @Test
    void persistsAndAuditsEvenWhenAllVersionsWereAlreadyArchived() {
        SimulationVersionEntity archived = version(3, SimulationVersionStatus.ARCHIVED);
        SimulationEntity simulation = simulation("simulation-2", archived);

        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(simulationRepository.findByEmpresaIdAndId("empresa-1", "simulation-2"))
                .thenReturn(Optional.of(simulation));

        service.archiveSimulation("simulation-2");

        verify(simulationRepository).save(simulation);
        verify(auditEventService).appendSimulationVersionEvent(
                "empresa-1",
                "simulation-2",
                3,
                AuditEventType.SIMULATION_ARCHIVED,
                "Avaliação arquivada sem exclusão de histórico.",
                "{\"status\":\"archived\",\"archivedVersions\":0}"
        );
    }

    private SimulationEntity simulation(String id, SimulationVersionEntity... versions) {
        SimulationEntity simulation = new SimulationEntity();
        simulation.setId(id);
        LinkedHashSet<SimulationVersionEntity> versionSet = new LinkedHashSet<>();
        for (SimulationVersionEntity version : versions) {
            versionSet.add(version);
        }
        simulation.setVersions(versionSet);
        return simulation;
    }

    private SimulationVersionEntity version(int number, SimulationVersionStatus status) {
        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setVersionNumber(number);
        version.setStatus(status);
        return version;
    }
}
