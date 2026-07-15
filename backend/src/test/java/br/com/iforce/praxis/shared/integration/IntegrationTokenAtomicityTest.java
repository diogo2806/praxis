package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.model.IntegrationType;
import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;
import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@Sql(scripts = "/seed-simulation-fixture.sql")
class IntegrationTokenAtomicityTest {

    @Autowired
    private IntegrationManagementService integrationManagementService;

    @Autowired
    private IntegrationTokenRepository integrationTokenRepository;

    @MockitoBean
    private CurrentEmpresaService currentEmpresaService;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private EmpresaIntegrationRepository empresaIntegrationRepository;

    @MockitoBean
    private AuditEventService auditEventService;

    @Test
    void rotationRollsBackRealTokenWhenOperationalStateCannotBePersisted() {
        String empresaId = "empresa-1";
        String previousHash = integrationTokenRepository
                .findByEmpresaIdOrderByProviderAsc(empresaId)
                .stream()
                .filter(token -> token.getProvider().equals("gupy"))
                .findFirst()
                .orElseThrow()
                .getTokenHash();

        EmpresaIntegrationEntity integration = new EmpresaIntegrationEntity();
        integration.setProvider(IntegrationProvider.GUPY);
        integration.setType(IntegrationType.ATS);
        integration.setStatus(IntegrationStatus.CONECTADA);
        integration.setCredentialsHash(previousHash);
        integration.setTokenPreview("token-antigo****");
        integration.setLastSyncAt(Instant.parse("2026-07-14T10:00:00Z"));
        integration.setCreatedAt(Instant.parse("2026-06-01T10:00:00Z"));
        integration.setUpdatedAt(Instant.parse("2026-07-14T10:00:00Z"));

        when(currentEmpresaService.requiredEmpresaId()).thenReturn(empresaId);
        when(currentUserService.requiredUserId()).thenReturn("usuario-1");
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(
                empresaId,
                IntegrationProvider.GUPY
        )).thenReturn(Optional.of(integration));
        when(empresaIntegrationRepository.save(any(EmpresaIntegrationEntity.class)))
                .thenThrow(new DataIntegrityViolationException("falha simulada em empresa_integrations"));

        assertThatThrownBy(() -> integrationManagementService.generateToken("gupy"))
                .isInstanceOf(DataIntegrityViolationException.class);

        var tokensAfterRollback = integrationTokenRepository
                .findByEmpresaIdOrderByProviderAsc(empresaId)
                .stream()
                .filter(token -> token.getProvider().equals("gupy"))
                .toList();
        assertThat(tokensAfterRollback).hasSize(1);
        assertThat(tokensAfterRollback.getFirst().getTokenHash()).isEqualTo(previousHash);
    }
}
