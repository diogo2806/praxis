package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.shared.integration.dto.IntegrationResponse;
import br.com.iforce.praxis.shared.integration.model.IntegrationAction;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Expõe o estado comprovável das integrações sem transformar a existência de um token
 * em evidência de comunicação com o ATS.
 */
@Service
public class IntegrationStatusRefreshService {

    private final IntegrationManagementService integrationManagementService;

    public IntegrationStatusRefreshService(IntegrationManagementService integrationManagementService) {
        this.integrationManagementService = integrationManagementService;
    }

    public List<IntegrationResponse> listIntegrations() {
        return integrationManagementService.listIntegrations().stream()
                .map(this::normalize)
                .toList();
    }

    public IntegrationResponse getIntegration(String provider) {
        return normalize(integrationManagementService.getIntegration(provider));
    }

    /**
     * Atualiza a visão consultando novamente o estado persistido. Não executa tráfego fictício
     * e não promove a integração para CONECTADA.
     */
    public IntegrationResponse refreshStatus(String provider) {
        return getIntegration(provider);
    }

    public IntegrationResponse normalize(IntegrationResponse response) {
        if (response == null || !isAtsProvider(response.provider())) {
            return response;
        }

        IntegrationStatus effectiveStatus = response.status();
        if (effectiveStatus == IntegrationStatus.CONECTADA && response.lastSyncAt() == null) {
            effectiveStatus = IntegrationStatus.PENDENTE;
        }

        return new IntegrationResponse(
                response.provider(),
                response.name(),
                response.description(),
                response.type(),
                effectiveStatus,
                response.lastSyncAt(),
                response.configuredAt(),
                response.errorMessage(),
                response.tokenPreview(),
                response.token(),
                response.tokenCreatedAt(),
                actionsFor(effectiveStatus)
        );
    }

    private List<IntegrationAction> actionsFor(IntegrationStatus status) {
        return switch (status) {
            case NAO_CONFIGURADA -> List.of(IntegrationAction.GENERATE_TOKEN);
            case PENDENTE -> List.of(
                    IntegrationAction.VIEW,
                    IntegrationAction.TEST_CONNECTION,
                    IntegrationAction.GENERATE_TOKEN
            );
            case CONECTADA -> List.of(
                    IntegrationAction.VIEW,
                    IntegrationAction.TEST_CONNECTION,
                    IntegrationAction.DISCONNECT
            );
            case ERRO -> List.of(
                    IntegrationAction.VIEW_ERROR,
                    IntegrationAction.TEST_CONNECTION,
                    IntegrationAction.GENERATE_TOKEN
            );
            case DESATIVADA -> List.of(IntegrationAction.REACTIVATE, IntegrationAction.VIEW);
        };
    }

    private boolean isAtsProvider(IntegrationProvider provider) {
        return provider == IntegrationProvider.GUPY || provider == IntegrationProvider.RECRUTEI;
    }
}
