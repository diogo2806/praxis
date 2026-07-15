package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.shared.integration.dto.IntegrationResponse;
import br.com.iforce.praxis.shared.integration.model.IntegrationAction;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.model.IntegrationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegrationStatusRefreshServiceTest {

    private final IntegrationManagementService integrationManagementService = mock(IntegrationManagementService.class);
    private final IntegrationStatusRefreshService service = new IntegrationStatusRefreshService(
            integrationManagementService
    );

    @Test
    void refreshKeepsTokenOnlyIntegrationPending() {
        IntegrationResponse pending = response(IntegrationStatus.PENDENTE, null);
        when(integrationManagementService.getIntegration("gupy")).thenReturn(pending);

        IntegrationResponse refreshed = service.refreshStatus("gupy");

        assertThat(refreshed.status()).isEqualTo(IntegrationStatus.PENDENTE);
        assertThat(refreshed.availableActions()).containsExactly(
                IntegrationAction.VIEW,
                IntegrationAction.TEST_CONNECTION,
                IntegrationAction.GENERATE_TOKEN
        );
        verify(integrationManagementService).getIntegration("gupy");
    }

    @Test
    void legacyConnectedWithoutActivityIsShownAsPending() {
        IntegrationResponse falsePositive = response(IntegrationStatus.CONECTADA, null);

        IntegrationResponse normalized = service.normalize(falsePositive);

        assertThat(normalized.status()).isEqualTo(IntegrationStatus.PENDENTE);
        assertThat(normalized.lastSyncAt()).isNull();
    }

    @Test
    void authenticatedActivityKeepsIntegrationConnected() {
        Instant lastActivity = Instant.parse("2026-07-15T10:00:00Z");
        IntegrationResponse connected = response(IntegrationStatus.CONECTADA, lastActivity);

        IntegrationResponse normalized = service.normalize(connected);

        assertThat(normalized.status()).isEqualTo(IntegrationStatus.CONECTADA);
        assertThat(normalized.lastSyncAt()).isEqualTo(lastActivity);
        assertThat(normalized.availableActions()).containsExactly(
                IntegrationAction.VIEW,
                IntegrationAction.TEST_CONNECTION,
                IntegrationAction.DISCONNECT
        );
    }

    @Test
    void nonAtsIntegrationIsNotChanged() {
        IntegrationResponse customApi = new IntegrationResponse(
                IntegrationProvider.CUSTOM_API,
                "API própria",
                "Webhook personalizado",
                IntegrationType.API,
                IntegrationStatus.CONECTADA,
                null,
                Instant.parse("2026-07-15T09:00:00Z"),
                null,
                null,
                null,
                null,
                List.of(IntegrationAction.VIEW)
        );

        assertThat(service.normalize(customApi)).isSameAs(customApi);
    }

    private IntegrationResponse response(IntegrationStatus status, Instant lastSyncAt) {
        return new IntegrationResponse(
                IntegrationProvider.GUPY,
                "Gupy",
                "Integração ATS",
                IntegrationType.ATS,
                status,
                lastSyncAt,
                Instant.parse("2026-07-15T09:00:00Z"),
                null,
                "prx_abcd****",
                null,
                null,
                List.of(IntegrationAction.GENERATE_TOKEN)
        );
    }
}
