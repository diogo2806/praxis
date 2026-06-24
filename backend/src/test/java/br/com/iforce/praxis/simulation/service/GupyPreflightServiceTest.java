package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.simulation.dto.GupyPreflightCheckResponse;
import br.com.iforce.praxis.simulation.dto.GupyPreflightResponse;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.model.GupyPreflightCheckCode;
import br.com.iforce.praxis.simulation.model.GupyPreflightCheckStatus;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GupyPreflightServiceTest {

    private final SimulationVersionRepository simulationVersionRepository = mock(SimulationVersionRepository.class);
    private final SimulationValidationService simulationValidationService = mock(SimulationValidationService.class);
    private final PraxisProperties praxisProperties = new PraxisProperties(
            "http://localhost:8080", 168, 24, 70, 15, 0.001
    );
    private final CurrentTenantService currentTenantService = mock(CurrentTenantService.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);

    private final GupyPreflightService service = new GupyPreflightService(
            simulationVersionRepository,
            simulationValidationService,
            praxisProperties,
            currentTenantService,
            tenantRepository
    );

    @Test
    void integrationTokenIsWarningNotBlockerWhenNotConfigured() {
        SimulationVersionEntity version = version(SimulationVersionStatus.DRAFT);
        stubTenant(null);
        stubPublishableValidation(version);

        GupyPreflightResponse response = service.evaluate(version);

        GupyPreflightCheckResponse tokenCheck = checkFor(response, GupyPreflightCheckCode.INTEGRATION_TOKEN);
        assertThat(tokenCheck.status()).isEqualTo(GupyPreflightCheckStatus.WARNING);
        assertThat(response.ok())
                .as("ausência de token Gupy não deve bloquear a publicação")
                .isTrue();
    }

    @Test
    void evaluateStaysBlockedWhenSimulationHasBlockers() {
        SimulationVersionEntity version = version(SimulationVersionStatus.DRAFT);
        stubTenant("hash-presente");
        when(simulationValidationService.validate(version))
                .thenReturn(new SimulationValidationResponse("sim-1", 1, false, 1, 0, 50, List.of()));

        GupyPreflightResponse response = service.evaluate(version);

        assertThat(response.ok()).isFalse();
        assertThat(checkFor(response, GupyPreflightCheckCode.SIMULATION_VALIDATION).status())
                .isEqualTo(GupyPreflightCheckStatus.BLOCKER);
    }

    @Test
    void getPreflightRunsOnDraftVersions() {
        SimulationVersionEntity version = version(SimulationVersionStatus.DRAFT);
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(simulationVersionRepository
                .findBySimulationTenantIdAndSimulationIdAndVersionNumber("tenant-1", "sim-1", 1))
                .thenReturn(Optional.of(version));
        stubTenant("hash-presente");
        stubPublishableValidation(version);

        GupyPreflightResponse response = service.getPreflight("sim-1", 1);

        assertThat(response.ok()).isTrue();
        assertThat(response.simulationId()).isEqualTo("sim-1");
    }

    @Test
    void getPreflightRejectsArchivedVersions() {
        SimulationVersionEntity version = version(SimulationVersionStatus.ARCHIVED);
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(simulationVersionRepository
                .findBySimulationTenantIdAndSimulationIdAndVersionNumber("tenant-1", "sim-1", 1))
                .thenReturn(Optional.of(version));

        assertThatThrownBy(() -> service.getPreflight("sim-1", 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("arquivadas");
    }

    private SimulationVersionEntity version(SimulationVersionStatus status) {
        SimulationEntity simulation = mock(SimulationEntity.class);
        when(simulation.getId()).thenReturn("sim-1");
        SimulationVersionEntity version = mock(SimulationVersionEntity.class);
        when(version.getStatus()).thenReturn(status);
        when(version.getSimulation()).thenReturn(simulation);
        when(version.getVersionNumber()).thenReturn(1);
        return version;
    }

    private void stubTenant(String integrationTokenHash) {
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        TenantEntity tenant = mock(TenantEntity.class);
        when(tenant.getIntegrationTokenHash()).thenReturn(integrationTokenHash);
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));
    }

    private void stubPublishableValidation(SimulationVersionEntity version) {
        when(simulationValidationService.validate(any(SimulationVersionEntity.class)))
                .thenReturn(new SimulationValidationResponse("sim-1", 1, true, 0, 0, 95, List.of()));
    }

    private GupyPreflightCheckResponse checkFor(GupyPreflightResponse response, GupyPreflightCheckCode code) {
        return response.checks().stream()
                .filter(check -> check.code() == code)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Check ausente: " + code));
    }
}
