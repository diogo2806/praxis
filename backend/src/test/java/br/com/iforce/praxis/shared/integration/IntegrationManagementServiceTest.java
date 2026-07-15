package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegrationManagementServiceTest {

    private final CurrentEmpresaService currentEmpresaService = mock(CurrentEmpresaService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final EmpresaIntegrationRepository empresaIntegrationRepository = mock(EmpresaIntegrationRepository.class);
    private final IntegrationTokenAdminService integrationTokenAdminService = mock(IntegrationTokenAdminService.class);
    private final AuditEventService auditEventService = mock(AuditEventService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final IntegrationManagementService service = new IntegrationManagementService(
            currentEmpresaService,
            currentUserService,
            empresaRepository,
            empresaIntegrationRepository,
            integrationTokenAdminService,
            auditEventService,
            objectMapper
    );

    @Test
    void unconfiguredGupyOffersGenerateTokenNotConfigure() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                "empresa-1",
                IntegrationProvider.GUPY
        )).thenReturn(Optional.empty());

        var response = service.getIntegration("gupy");

        assertThat(response.availableActions()).containsExactly(IntegrationAction.GENERATE_TOKEN);
    }

    @Test
    void unconfiguredRecruteiOffersGenerateTokenNotConfigure() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                "empresa-1",
                IntegrationProvider.RECRUTEI
        )).thenReturn(Optional.empty());

        var response = service.getIntegration("recrutei");

        assertThat(response.availableActions()).containsExactly(IntegrationAction.GENERATE_TOKEN);
    }

    @Test
    void configureRejectsGupyAndPointsToTokenEndpoint() {
        assertThatThrownBy(() -> service.configure(
                "gupy",
                new ConfigureIntegrationRequest(Map.of(), Map.of())
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("/tokens");
    }

    @Test
    void configureRejectsRecrutei() {
        assertThatThrownBy(() -> service.configure(
                "recrutei",
                new ConfigureIntegrationRequest(Map.of(), Map.of())
        )).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void rotatingConnectedAtsResetsPreviousCredentialEvidenceAndKeepsTablesCoherent() {
        String empresaId = "empresa-1";
        String rawToken = "prx_abcdefghijklmnopqrstuvwxyz0123456789";
        Instant tokenCreatedAt = Instant.parse("2026-07-15T12:00:00Z");
        EmpresaEntity empresa = empresa();
        EmpresaIntegrationEntity entity = integration(IntegrationStatus.CONECTADA);
        entity.setCredentialsHash("hash-antigo");
        entity.setCredentialsEncrypted("credencial-antiga-cifrada");
        entity.setTokenPreview("token-antigo****");
        entity.setLastSyncAt(Instant.parse("2026-07-14T10:00:00Z"));
        entity.setLastErrorMessage("erro da credencial anterior");

        when(currentEmpresaService.requiredEmpresaId()).thenReturn(empresaId);
        when(currentUserService.requiredUserId()).thenReturn("usuario-1");
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa));
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                empresaId,
                IntegrationProvider.GUPY
        )).thenReturn(Optional.of(entity));
        when(integrationTokenAdminService.rotateToken("gupy"))
                .thenReturn(new RotateIntegrationTokenResponse("gupy", true, tokenCreatedAt, rawToken));
        saveReturnsArgument();

        var response = service.generateToken("gupy");

        assertThat(response.token()).isEqualTo(rawToken);
        assertThat(response.createdAt()).isEqualTo(tokenCreatedAt);
        assertThat(response.tokenPreview()).isEqualTo("prx_abcd****");
        assertThat(entity.getStatus()).isEqualTo(IntegrationStatus.PENDENTE);
        assertThat(entity.getCredentialsHash())
                .isNotBlank()
                .isNotEqualTo(rawToken)
                .isNotEqualTo("hash-antigo");
        assertThat(entity.getCredentialsEncrypted()).isNull();
        assertThat(entity.getTokenPreview()).isEqualTo("prx_abcd****");
        assertThat(entity.getLastSyncAt()).isNull();
        assertThat(entity.getLastErrorMessage()).isNull();
        assertThat(entity.getDisabledAt()).isNull();
        assertThat(entity.getConfiguredAt()).isNotNull();
        verify(integrationTokenAdminService).rotateToken("gupy");
        verify(auditEventService).appendIntegrationEvent(
                eq(empresaId),
                eq("usuario-1"),
                eq("GUPY"),
                eq(AuditEventType.INTEGRATION_TOKEN_ROTATED),
                anyString(),
                anyString()
        );
    }

    @Test
    void revokingTokenDisablesIntegrationAndRemovesAllDisplayedCredentialEvidence() {
        String empresaId = "empresa-1";
        EmpresaIntegrationEntity entity = integration(IntegrationStatus.CONECTADA);
        entity.setCredentialsHash("hash-antigo");
        entity.setCredentialsEncrypted("credencial-antiga-cifrada");
        entity.setTokenPreview("token-antigo****");
        entity.setLastSyncAt(Instant.parse("2026-07-14T10:00:00Z"));
        entity.setLastErrorMessage("erro anterior");

        when(currentEmpresaService.requiredEmpresaId()).thenReturn(empresaId);
        when(currentUserService.requiredUserId()).thenReturn("usuario-1");
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa()));
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                empresaId,
                IntegrationProvider.GUPY
        )).thenReturn(Optional.of(entity));
        saveReturnsArgument();

        service.revokeProviderToken("gupy");

        assertThat(entity.getStatus()).isEqualTo(IntegrationStatus.DESATIVADA);
        assertThat(entity.getCredentialsHash()).isNull();
        assertThat(entity.getCredentialsEncrypted()).isNull();
        assertThat(entity.getTokenPreview()).isNull();
        assertThat(entity.getLastSyncAt()).isNull();
        assertThat(entity.getLastErrorMessage()).isNull();
        assertThat(entity.getDisabledAt()).isNotNull();
        verify(integrationTokenAdminService).revokeToken("gupy");
        verify(auditEventService).appendIntegrationEvent(
                eq(empresaId),
                eq("usuario-1"),
                eq("GUPY"),
                eq(AuditEventType.INTEGRATION_TOKEN_REVOKED),
                anyString(),
                anyString()
        );
    }

    @Test
    void testConnectionOnlyReadsPersistedStateAndNeverPromotesByHash() {
        EmpresaIntegrationEntity entity = integration(IntegrationStatus.PENDENTE);
        entity.setCredentialsHash("hash-sem-atividade-externa");
        entity.setTokenPreview("prx_abcd****");

        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                "empresa-1",
                IntegrationProvider.GUPY
        )).thenReturn(Optional.of(entity));

        var response = service.testConnection("gupy");

        assertThat(response.status()).isEqualTo(IntegrationStatus.PENDENTE);
        assertThat(response.lastSyncAt()).isNull();
        verify(empresaIntegrationRepository, never()).save(any(EmpresaIntegrationEntity.class));
    }

    @Test
    void reactivateGupyReturnsNewTokenOnlyOnceAndKeepsIntegrationPending() {
        String empresaId = "empresa-1";
        String rawToken = "prx_abcdefghijklmnopqrstuvwxyz0123456789";
        Instant tokenCreatedAt = Instant.parse("2026-07-14T12:00:00Z");
        EmpresaIntegrationEntity entity = integration(IntegrationStatus.DESATIVADA);
        entity.setCredentialsHash("hash-antigo");
        entity.setCredentialsEncrypted("credencial-antiga-cifrada");
        entity.setTokenPreview("token-antigo****");
        entity.setLastSyncAt(Instant.parse("2026-07-01T10:00:00Z"));
        entity.setLastErrorMessage("erro anterior");

        when(currentEmpresaService.requiredEmpresaId()).thenReturn(empresaId);
        when(currentUserService.requiredUserId()).thenReturn("usuario-1");
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                empresaId,
                IntegrationProvider.GUPY
        )).thenReturn(Optional.of(entity));
        when(integrationTokenAdminService.rotateToken("gupy"))
                .thenReturn(new RotateIntegrationTokenResponse("gupy", true, tokenCreatedAt, rawToken));
        saveReturnsArgument();

        var response = service.reactivate("gupy");

        assertThat(response.status()).isEqualTo(IntegrationStatus.PENDENTE);
        assertThat(response.token()).isEqualTo(rawToken);
        assertThat(response.tokenCreatedAt()).isEqualTo(tokenCreatedAt);
        assertThat(response.tokenPreview()).isEqualTo("prx_abcd****");
        assertThat(response.availableActions()).containsExactly(IntegrationAction.GENERATE_TOKEN);
        assertThat(entity.getStatus()).isEqualTo(IntegrationStatus.PENDENTE);
        assertThat(entity.getCredentialsHash())
                .isNotBlank()
                .isNotEqualTo(rawToken)
                .isNotEqualTo("hash-antigo");
        assertThat(entity.getCredentialsEncrypted()).isNull();
        assertThat(entity.getTokenPreview()).isEqualTo("prx_abcd****");
        assertThat(entity.getLastSyncAt()).isNull();
        assertThat(entity.getLastErrorMessage()).isNull();
        assertThat(entity.getDisabledAt()).isNull();
        assertThat(entity.getConfiguredAt()).isNotNull();
        verify(integrationTokenAdminService).rotateToken("gupy");

        var laterResponse = service.getIntegration("gupy");
        assertThat(laterResponse.status()).isEqualTo(IntegrationStatus.PENDENTE);
        assertThat(laterResponse.token()).isNull();
        assertThat(laterResponse.tokenCreatedAt()).isNull();
    }

    @Test
    void revokingLegacyOrphanTokenCreatesDisabledOperationalState() {
        String empresaId = "empresa-1";
        when(currentEmpresaService.requiredEmpresaId()).thenReturn(empresaId);
        when(currentUserService.requiredUserId()).thenReturn("usuario-1");
        when(empresaRepository.findById(empresaId)).thenReturn(Optional.of(empresa()));
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                empresaId,
                IntegrationProvider.RECRUTEI
        )).thenReturn(Optional.empty());
        saveReturnsArgument();

        service.revokeProviderToken("recrutei");

        ArgumentCaptor<EmpresaIntegrationEntity> captor = ArgumentCaptor.forClass(EmpresaIntegrationEntity.class);
        verify(empresaIntegrationRepository).save(captor.capture());
        EmpresaIntegrationEntity saved = captor.getValue();
        assertThat(saved.getProvider()).isEqualTo(IntegrationProvider.RECRUTEI);
        assertThat(saved.getStatus()).isEqualTo(IntegrationStatus.DESATIVADA);
        assertThat(saved.getCredentialsHash()).isNull();
        assertThat(saved.getTokenPreview()).isNull();
        verify(integrationTokenAdminService).revokeToken("recrutei");
    }

    @Test
    void pendingAuthenticatedActivityConnectsAndAuditsEndpointEvidence() throws Exception {
        EmpresaIntegrationEntity entity = integration(IntegrationStatus.PENDENTE);
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                "empresa-1",
                IntegrationProvider.GUPY
        )).thenReturn(Optional.of(entity));
        saveReturnsArgument();
        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);

        service.recordActivity("empresa-1", IntegrationProvider.GUPY, "GET /test");

        assertThat(entity.getStatus()).isEqualTo(IntegrationStatus.CONECTADA);
        assertThat(entity.getLastSyncAt()).isNotNull();
        assertThat(entity.getLastErrorMessage()).isNull();
        verify(auditEventService).appendIntegrationEvent(
                eq("empresa-1"),
                isNull(),
                eq("GUPY"),
                eq(AuditEventType.INTEGRATION_CONNECTED),
                anyString(),
                metadataCaptor.capture()
        );
        JsonNode metadata = objectMapper.readTree(metadataCaptor.getValue());
        assertThat(metadata.get("provider").asText()).isEqualTo("GUPY");
        assertThat(metadata.get("endpoint").asText()).isEqualTo("GET /test");
        assertThat(metadata.get("statusAnterior").asText()).isEqualTo("PENDENTE");
        assertThat(metadata.get("statusNovo").asText()).isEqualTo("CONECTADA");
        assertThat(metadata.get("dataHora").asText()).isNotBlank();
    }

    @Test
    void errorAuthenticatedActivityRecoversAndAuditsTransition() {
        EmpresaIntegrationEntity entity = integration(
                IntegrationProvider.RECRUTEI,
                IntegrationStatus.ERRO
        );
        entity.setLastErrorMessage("Falha anterior");
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                "empresa-1",
                IntegrationProvider.RECRUTEI
        )).thenReturn(Optional.of(entity));
        saveReturnsArgument();

        service.recordActivity(
                "empresa-1",
                IntegrationProvider.RECRUTEI,
                "GET /recrutei/test/result/{resultId}"
        );

        assertThat(entity.getStatus()).isEqualTo(IntegrationStatus.CONECTADA);
        assertThat(entity.getLastErrorMessage()).isNull();
        assertThat(entity.getLastSyncAt()).isNotNull();
        verify(auditEventService).appendIntegrationEvent(
                eq("empresa-1"),
                isNull(),
                eq("RECRUTEI"),
                eq(AuditEventType.INTEGRATION_RECOVERED),
                anyString(),
                anyString()
        );
    }

    @Test
    void connectedAuthenticatedActivityRefreshesLastSyncAndAuditsRequest() {
        EmpresaIntegrationEntity entity = integration(IntegrationStatus.CONECTADA);
        Instant previousSync = Instant.parse("2026-07-01T10:00:00Z");
        entity.setLastSyncAt(previousSync);
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                "empresa-1",
                IntegrationProvider.GUPY
        )).thenReturn(Optional.of(entity));
        saveReturnsArgument();

        service.recordActivity("empresa-1", IntegrationProvider.GUPY, "POST /test/candidate");

        assertThat(entity.getStatus()).isEqualTo(IntegrationStatus.CONECTADA);
        assertThat(entity.getLastSyncAt()).isAfter(previousSync);
        verify(auditEventService).appendIntegrationEvent(
                eq("empresa-1"),
                isNull(),
                eq("GUPY"),
                eq(AuditEventType.INTEGRATION_ACTIVITY_RECORDED),
                anyString(),
                anyString()
        );
    }

    @Test
    void disabledIntegrationIsNeverReactivatedByAuthenticatedActivity() {
        EmpresaIntegrationEntity entity = integration(IntegrationStatus.DESATIVADA);
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                "empresa-1",
                IntegrationProvider.GUPY
        )).thenReturn(Optional.of(entity));

        assertThatThrownBy(() ->
                service.recordActivity("empresa-1", IntegrationProvider.GUPY, "GET /test")
        )
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("desativada");

        assertThat(entity.getStatus()).isEqualTo(IntegrationStatus.DESATIVADA);
        assertThat(entity.getLastSyncAt()).isNull();
        verify(empresaIntegrationRepository, never()).save(any());
        verify(auditEventService, never()).appendIntegrationEvent(
                anyString(), any(), anyString(), any(), anyString(), anyString()
        );
    }

    @Test
    void internalActivityCannotPromoteAtsWithoutEndpointEvidence() {
        service.recordActivity("empresa-1", IntegrationProvider.GUPY);

        verify(empresaIntegrationRepository, never())
                .findFirstByEmpresaIdAndProvider(anyString(), any());
        verify(empresaIntegrationRepository, never()).save(any());
    }

    private static EmpresaIntegrationEntity integration(IntegrationStatus status) {
        return integration(IntegrationProvider.GUPY, status);
    }

    private static EmpresaIntegrationEntity integration(
            IntegrationProvider provider,
            IntegrationStatus status
    ) {
        EmpresaIntegrationEntity entity = new EmpresaIntegrationEntity();
        entity.setEmpresa(empresa());
        entity.setProvider(provider);
        entity.setType(IntegrationType.ATS);
        entity.setStatus(status);
        entity.setCreatedAt(Instant.parse("2026-06-01T10:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-07-01T10:00:00Z"));
        return entity;
    }

    private static EmpresaEntity empresa() {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("empresa-1");
        empresa.setName("Acme");
        empresa.setCompanyId("1");
        return empresa;
    }

    private void saveReturnsArgument() {
        when(empresaIntegrationRepository.save(any(EmpresaIntegrationEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
