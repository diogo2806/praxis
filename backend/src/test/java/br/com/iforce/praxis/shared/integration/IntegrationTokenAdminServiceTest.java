package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

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

    private final CurrentEmpresaService currentEmpresaService = mock(CurrentEmpresaService.class);
    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final IntegrationTokenRepository integrationTokenRepository = mock(IntegrationTokenRepository.class);
    private final IntegrationTokenAdminService service = new IntegrationTokenAdminService(
            currentEmpresaService,
            empresaRepository,
            integrationTokenRepository
    );

    @Test
    void listTokensReturnsSupportedProvidersWithoutSecrets() {
        IntegrationTokenEntity gupyToken = token("gupy", "hash");
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(integrationTokenRepository.findByEmpresaIdOrderByProviderAsc("empresa-1"))
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
        EmpresaEntity empresa = empresa();
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(empresaRepository.findById("empresa-1")).thenReturn(Optional.of(empresa));
        when(integrationTokenRepository.save(any(IntegrationTokenEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RotateIntegrationTokenResponse response = service.rotateToken("gupy");
        ArgumentCaptor<IntegrationTokenEntity> tokenCaptor = ArgumentCaptor.forClass(IntegrationTokenEntity.class);
        verify(integrationTokenRepository).save(tokenCaptor.capture());
        IntegrationTokenEntity saved = tokenCaptor.getValue();

        assertThat(response.provider()).isEqualTo("gupy");
        assertThat(response.token()).startsWith("prx_");
        verify(integrationTokenRepository).deleteByEmpresaIdAndProvider("empresa-1", "gupy");

        IntegrationTokenRepository authRepository = mock(IntegrationTokenRepository.class);
        when(authRepository.findFirstByProviderAndTokenHash("gupy", saved.getTokenHash()))
                .thenReturn(Optional.of(saved));

        IntegrationEmpresaContext context = new IntegrationAuthService(authRepository)
                .validateBearerToken("Bearer " + response.token(), "gupy");

        assertThat(saved.getTokenHash()).isNotEqualTo(response.token());
        assertThat(context.empresaId()).isEqualTo("empresa-1");
        assertThat(context.companyId()).isEqualTo("empresa-123");
    }

    @Test
    void revokeTokenDeletesOnlyCurrentEmpresaProvider() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");

        service.revokeToken("recrutei");

        verify(integrationTokenRepository).deleteByEmpresaIdAndProvider("empresa-1", "recrutei");
    }

    private static IntegrationTokenEntity token(String provider, String hash) {
        IntegrationTokenEntity token = new IntegrationTokenEntity();
        token.setProvider(provider);
        token.setTokenHash(hash);
        token.setCreatedAt(java.time.Instant.now());
        token.setEmpresa(empresa());
        return token;
    }

    private static EmpresaEntity empresa() {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("empresa-1");
        empresa.setName("Acme");
        empresa.setCompanyId("empresa-123");
        return empresa;
    }
}
