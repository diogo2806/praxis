package br.com.iforce.praxis.shared.integration.dto;

import br.com.iforce.praxis.shared.integration.model.IntegrationAction;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.model.IntegrationType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrationResponseTest {

    @Test
    void disabledIntegrationDoesNotExposeDisconnectAction() {
        IntegrationResponse response = response(
                IntegrationStatus.DESATIVADA,
                List.of(
                        IntegrationAction.REACTIVATE,
                        IntegrationAction.VIEW,
                        IntegrationAction.DISCONNECT
                )
        );

        assertThat(response.availableActions()).containsExactly(
                IntegrationAction.REACTIVATE,
                IntegrationAction.VIEW
        );
    }

    @Test
    void connectedIntegrationKeepsDisconnectAction() {
        IntegrationResponse response = response(
                IntegrationStatus.CONECTADA,
                List.of(IntegrationAction.VIEW, IntegrationAction.DISCONNECT)
        );

        assertThat(response.availableActions()).containsExactly(
                IntegrationAction.VIEW,
                IntegrationAction.DISCONNECT
        );
    }

    private IntegrationResponse response(
            IntegrationStatus status,
            List<IntegrationAction> availableActions
    ) {
        return new IntegrationResponse(
                IntegrationProvider.GUPY,
                "Gupy",
                "Integração ATS",
                IntegrationType.ATS,
                status,
                null,
                null,
                null,
                null,
                null,
                null,
                availableActions
        );
    }
}
