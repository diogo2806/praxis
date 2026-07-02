package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.shared.integration.dto.ConfigureIntegrationRequest;
import br.com.iforce.praxis.shared.integration.model.IntegrationAction;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntegrationManagementServiceTest {

    private final CurrentEmpresaService currentEmpresaService = mock(CurrentEmpresaService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final EmpresaIntegrationRepository empresaIntegrationRepository = mock(EmpresaIntegrationRepository.class);
    private final IntegrationTokenAdminService integrationTokenAdminService = mock(IntegrationTokenAdminService.class);
    private final AuditEventService auditEventService = mock(AuditEventService.class);
    private final IntegrationManagementService service = new IntegrationManagementService(
            currentEmpresaService,
            currentUserService,
            empresaRepository,
            empresaIntegrationRepository,
            integrationTokenAdminService,
            auditEventService,
            new ObjectMapper()
    );

    @Test
    void unconfiguredGupyOffersGenerateTokenNotConfigure() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider("empresa-1", IntegrationProvider.GUPY))
                .thenReturn(Optional.empty());

        var response = service.getIntegration("gupy");

        assertThat(response.availableActions()).containsExactly(IntegrationAction.GENERATE_TOKEN);
    }

    @Test
    void unconfiguredRecruteiOffersGenerateTokenNotConfigure() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider("empresa-1", IntegrationProvider.RECRUTEI))
                .thenReturn(Optional.empty());

        var response = service.getIntegration("recrutei");

        assertThat(response.availableActions()).containsExactly(IntegrationAction.GENERATE_TOKEN);
    }

    @Test
    void configureRejectsGupyAndPointsToTokenEndpoint() {
        assertThatThrownBy(() ->
                service.configure("gupy", new ConfigureIntegrationRequest(Map.of(), Map.of()))
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("/tokens");
    }

    @Test
    void configureRejectsRecrutei() {
        assertThatThrownBy(() ->
                service.configure("recrutei", new ConfigureIntegrationRequest(Map.of(), Map.of()))
        )
                .isInstanceOf(ResponseStatusException.class);
    }
}
