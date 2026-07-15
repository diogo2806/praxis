package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.shared.integration.dto.GenerateIntegrationTokenResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class IntegrationTokenAdminControllerTest {

    private final IntegrationTokenAdminService integrationTokenAdminService = mock(IntegrationTokenAdminService.class);
    private final IntegrationManagementService integrationManagementService = mock(IntegrationManagementService.class);
    private final IntegrationTokenAdminController controller = new IntegrationTokenAdminController(
            integrationTokenAdminService,
            integrationManagementService
    );

    @Test
    void rotateDelegatesToCentralTransactionalUseCase() {
        Instant createdAt = Instant.parse("2026-07-15T12:00:00Z");
        when(integrationManagementService.generateToken("gupy"))
                .thenReturn(new GenerateIntegrationTokenResponse(
                        "GUPY",
                        "prx_token-novo",
                        "prx_toke****",
                        createdAt
                ));

        var response = controller.rotateToken("gupy");

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().provider()).isEqualTo("gupy");
        assertThat(response.getBody().token()).isEqualTo("prx_token-novo");
        assertThat(response.getBody().createdAt()).isEqualTo(createdAt);
        verify(integrationManagementService).generateToken("gupy");
        verifyNoMoreInteractions(integrationTokenAdminService);
    }

    @Test
    void revokeDelegatesToCentralTransactionalUseCase() {
        var response = controller.revokeToken("recrutei");

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(integrationManagementService).revokeProviderToken("recrutei");
        verifyNoMoreInteractions(integrationTokenAdminService);
    }
}
