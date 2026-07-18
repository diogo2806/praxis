package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IntegrationAuthServicePartnerClientTest {

    @Test
    void tokenDeClienteUsaIdentificadorExternoSemAlterarEmpresaProprietaria() throws Exception {
        IntegrationTokenRepository repository = mock(IntegrationTokenRepository.class);
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("parceiro-1");
        empresa.setName("Parceiro");
        empresa.setCompanyId("company-parceiro");

        String rawToken = "prx_token_cliente";
        String tokenHash = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8))
        );

        IntegrationTokenEntity entity = new IntegrationTokenEntity();
        entity.setEmpresa(empresa);
        entity.setProvider("gupy");
        entity.setTokenHash(tokenHash);
        entity.setPartnerClientId("pcli_123");
        entity.setClientCompanyId("998877");

        when(repository.findFirstByProviderAndTokenHash("gupy", tokenHash))
                .thenReturn(Optional.of(entity));

        IntegrationEmpresaContext context = new IntegrationAuthService(repository)
                .validateBearerToken("Bearer " + rawToken, "gupy");

        assertThat(context.empresaId()).isEqualTo("parceiro-1");
        assertThat(context.companyId()).isEqualTo("998877");
        assertThat(context.partnerClientId()).isEqualTo("pcli_123");
        assertThat(context.provider()).isEqualTo("gupy");
    }
}
