package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.shared.integration.IntegrationTokenEntity;
import br.com.iforce.praxis.shared.integration.IntegrationTokenRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StrictGupyPreflightServiceTest {

    private final SimulationVersionRepository versionRepository = mock(SimulationVersionRepository.class);
    private final SimulationValidationService validationService = mock(SimulationValidationService.class);
    private final CurrentEmpresaService currentEmpresaService = mock(CurrentEmpresaService.class);
    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final IntegrationTokenRepository tokenRepository = mock(IntegrationTokenRepository.class);
    private final PraxisProperties properties = new PraxisProperties(
            "https://praxis.example.com", 168, 24, 70, 15, 0.001
    );

    private final StrictGupyPreflightService service = new StrictGupyPreflightService(
            versionRepository,
            validationService,
            properties,
            currentEmpresaService,
            empresaRepository,
            tokenRepository
    );

    @Test
    void missingTokenFromRealRepositoryBlocksActivation() {
        SimulationVersionEntity version = version(SimulationVersionStatus.PUBLISHED);
        stubBase(version);
        when(tokenRepository.findFirstByEmpresaIdAndProvider("empresa-1", "gupy"))
                .thenReturn(Optional.empty());

        GupyPreflightResponse response = service.getPreflight("sim-1", 1);

        assertThat(response.ok()).isFalse();
        assertThat(checkFor(response, GupyPreflightCheckCode.INTEGRATION_TOKEN).status())
                .isEqualTo(GupyPreflightCheckStatus.BLOCKER);
    }

    @Test
    void missingTokenDoesNotPreventPublishingForOtherChannels() {
        SimulationVersionEntity version = version(SimulationVersionStatus.DRAFT);
        stubBase(version);
        when(tokenRepository.findFirstByEmpresaIdAndProvider("empresa-1", "gupy"))
                .thenReturn(Optional.empty());

        GupyPreflightResponse response = service.evaluate(version);

        assertThat(response.ok()).isTrue();
        assertThat(checkFor(response, GupyPreflightCheckCode.INTEGRATION_TOKEN).status())
                .isEqualTo(GupyPreflightCheckStatus.WARNING);
    }

    @Test
    void publishedVersionWithRealTokenPassesActivation() {
        SimulationVersionEntity version = version(SimulationVersionStatus.PUBLISHED);
        stubBase(version);
        when(tokenRepository.findFirstByEmpresaIdAndProvider("empresa-1", "gupy"))
                .thenReturn(Optional.of(mock(IntegrationTokenEntity.class)));

        GupyPreflightResponse response = service.getPreflight("sim-1", 1);

        assertThat(response.ok()).isTrue();
        assertThat(checkFor(response, GupyPreflightCheckCode.PUBLICATION_STATUS).status())
                .isEqualTo(GupyPreflightCheckStatus.OK);
        assertThat(checkFor(response, GupyPreflightCheckCode.INTEGRATION_TOKEN).status())
                .isEqualTo(GupyPreflightCheckStatus.OK);
    }

    @Test
    void draftVersionCannotBeActivatedInGupy() {
        SimulationVersionEntity version = version(SimulationVersionStatus.DRAFT);
        stubBase(version);
        when(tokenRepository.findFirstByEmpresaIdAndProvider("empresa-1", "gupy"))
                .thenReturn(Optional.of(mock(IntegrationTokenEntity.class)));

        GupyPreflightResponse response = service.getPreflight("sim-1", 1);

        assertThat(response.ok()).isFalse();
        assertThat(checkFor(response, GupyPreflightCheckCode.PUBLICATION_STATUS).status())
                .isEqualTo(GupyPreflightCheckStatus.BLOCKER);
    }

    private void stubBase(SimulationVersionEntity version) {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(empresaRepository.findById("empresa-1")).thenReturn(Optional.empty());
        when(versionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                "empresa-1", "sim-1", 1
        )).thenReturn(Optional.of(version));
        when(validationService.validate(any(SimulationVersionEntity.class)))
                .thenReturn(new SimulationValidationResponse("sim-1", 1, true, 0, 0, 100, List.of()));
    }

    private SimulationVersionEntity version(SimulationVersionStatus status) {
        SimulationEntity simulation = mock(SimulationEntity.class);
        when(simulation.getId()).thenReturn("sim-1");
        SimulationVersionEntity version = mock(SimulationVersionEntity.class);
        when(version.getSimulation()).thenReturn(simulation);
        when(version.getVersionNumber()).thenReturn(1);
        when(version.getStatus()).thenReturn(status);
        return version;
    }

    private GupyPreflightCheckResponse checkFor(
            GupyPreflightResponse response,
            GupyPreflightCheckCode code
    ) {
        return response.checks().stream()
                .filter(check -> check.code() == code)
                .findFirst()
                .orElseThrow();
    }
}
