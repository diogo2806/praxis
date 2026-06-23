package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.shared.integration.dto.RotateIntegrationTokenResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class IntegrationTokenAdminServiceTest {

    private final CurrentTenantService currentTenantService = mock(CurrentTenantService.class);
    private final TenantRepository tenantRepository = mock(TenantRepository.class);
    private final IntegrationTokenRepository integrationTokenRepository = mock(IntegrationTokenRepository.class);
    private final IntegrationTokenAdminService service = new IntegrationTokenAdminService(
            currentTenantService,
            tenantRepository,
            integrationTokenRepository
    );

    @Test
    void listTokensReturnsSupportedProvidersWithoutSecrets() {
        IntegrationTokenEntity gupyToken = token("gupy", "hash");
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(integrationTokenRepository.findByTenantIdOrderByProviderAsc("tenant-1"))
                .thenReturn(List.of(gupyToken));

        var tokens = service.listTokens();

        assertThat(tokens).extracting("provider").containsExactly("gupy", "recrutei");
        assertThat(tokens).filteredOn(token -> token.provider().equals("gupy"))
                .singleElement()
                .satisfies(token -> {
                    assertThat(token.configured()).isTrue();
                    assertThat(token.createdAt()).isNotNull();
                });
        assertThat(tokens).filteredOn(token -> token.provider().equals("recrutei"))
                .singleElement()
                .satisfies(token -> assertThat(token.configured()).isFalse());
    }

    @Test
    void rotateTokenStoresOnlyHashAndCanBeValidatedByIntegrationAuthService() {
        TenantEntity tenant = tenant();
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));
        when(integrationTokenRepository.save(any(IntegrationTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RotateIntegrationTokenResponse response = service.rotateToken("gupy");
        ArgumentCaptor<IntegrationTokenEntity> tokenCaptor = ArgumentCaptor.forClass(IntegrationTokenEntity.class);
        verify(integrationTokenRepository).save(tokenCaptor.capture());
        IntegrationTokenEntity saved = tokenCaptor.getValue();

        assertThat(response.provider()).isEqualTo("gupy");
        assertThat(response.token()).startsWith("prx_");
        verify(integrationTokenRepository).deleteByTenantIdAndProvider("tenant-1", "gupy");

        IntegrationTokenRepository authRepository = mock(IntegrationTokenRepository.class);
        when(authRepository.findFirstByProviderAndTokenHash("gupy", saved.getTokenHash()))
                .thenReturn(Optional.of(saved));

        IntegrationTenantContext context = new IntegrationAuthService(authRepository)
                .validateBearerToken("Bearer " + response.token(), "gupy");

        assertThat(saved.getTokenHash()).isNotEqualTo(response.token());
        assertThat(context.tenantId()).isEqualTo("tenant-1");
        assertThat(context.companyId()).isEqualTo("empresa-123");
    }

    @Test
    void revokeTokenDeletesOnlyCurrentTenantProvider() {
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");

        service.revokeToken("recrutei");

        verify(integrationTokenRepository).deleteByTenantIdAndProvider("tenant-1", "recrutei");
    }

    private static IntegrationTokenEntity token(String provider, String hash) {
        IntegrationTokenEntity token = new IntegrationTokenEntity();
        token.setProvider(provider);
        token.setTokenHash(hash);
        token.setCreatedAt(java.time.Instant.now());
        token.setTenant(tenant());
        return token;
    }

    private static TenantEntity tenant() {
        TenantEntity tenant = new TenantEntity();
        tenant.setId("tenant-1");
        tenant.setName("Acme");
        tenant.setCompanyId("empresa-123");
        return tenant;
    }
}
