package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.shared.integration.dto.ConfigureIntegrationRequest;
import br.com.iforce.praxis.shared.integration.dto.RotateIntegrationTokenResponse;
import br.com.iforce.praxis.shared.integration.model.IntegrationAction;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.model.IntegrationType;
import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;
import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @Test
    void reactivateGupyReturnsNewTokenOnlyOnceAndKeepsIntegrationPending() {
        String empresaId = "empresa-1";
        String rawToken = "prx_abcdefghijklmnopqrstuvwxyz0123456789";
        Instant tokenCreatedAt = Instant.parse("2026-07-14T12:00:00Z");
        EmpresaIntegrationEntity entity = new EmpresaIntegrationEntity();
        entity.setProvider(IntegrationProvider.GUPY);
        entity.setType(IntegrationType.ATS);
        entity.setStatus(IntegrationStatus.DESATIVADA);
        entity.setCredentialsHash("hash-antigo");
        entity.setTokenPreview("token-antigo****");
        entity.setLastSyncAt(Instant.parse("2026-07-01T10:00:00Z"));
        entity.setCreatedAt(Instant.parse("2026-06-01T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-07-01T10:00:00Z"));

        when(currentEmpresaService.requiredEmpresaId()).thenReturn(empresaId);
        when(currentUserService.requiredUserId()).thenReturn("usuario-1");
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.GUPY))
      .thenReturn(Optional.of(entity));
        when(integrationTokenAdminService.rotateToken("gupy"))
      .thenReturn(new RotateIntegrationTokenResponse("gupy", true, tokenCreatedAt, rawToken));
        when(empresaIntegrationRepository.save(any(EmpresaIntegrationEntity.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.reactivate("gupy");

        assertThat(response.status()).isEqualTo(IntegrationStatus.PENDENTE);
        assertThat(response.token()).isEqualTo(rawToken);
        assertThat(response.tokenCreatedAt()).isEqualTo(tokenCreatedAt);
        assertThat(response.tokenPreview()).isEqualTo("prx_abcd****");
        assertThat(response.availableActions()).containsExactly(IntegrationAction.GENERATE_TOKEN);
        assertThat(entity.getStatus()).isEqualTo(IntegrationStatus.PENDENTE);
        assertThat(entity.getCredentialsHash()).isNotBlank().isNotEqualTo(rawToken).isNotEqualTo("hash-antigo");
        assertThat(entity.getTokenPreview()).isEqualTo("prx_abcd****");
        assertThat(entity.getLastSyncAt()).isNull();
        assertThat(entity.getDisabledAt()).isNull();
        assertThat(entity.getConfiguredAt()).isNotNull();
        verify(integrationTokenAdminService).rotateToken("gupy");

        var laterResponse = service.getIntegration("gupy");
        assertThat(laterResponse.status()).isEqualTo(IntegrationStatus.PENDENTE);
        assertThat(laterResponse.token()).isNull();
        assertThat(laterResponse.tokenCreatedAt()).isNull();
    }
}
