package br.com.iforce.praxis.shared.integration.service;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;
import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * Adapta a entrega CUSTOM_API legada, que registra falhas internamente, para um
 * contrato confirmável pelo outbox. Uma falha persistida volta a ser propagada
 * para que o processador mantenha a entrega em RETRYING/DLQ.
 */
@Service
public class ConfirmableGenericWebhookDeliveryService {

    private final GenericWebhookDeliveryService delegate;
    private final EmpresaIntegrationRepository integrationRepository;
    private final ObjectMapper objectMapper;

    public ConfirmableGenericWebhookDeliveryService(
            GenericWebhookDeliveryService delegate,
            EmpresaIntegrationRepository integrationRepository,
            ObjectMapper objectMapper
    ) {
        this.delegate = delegate;
        this.integrationRepository = integrationRepository;
        this.objectMapper = objectMapper;
    }

    public void deliverResultReady(String empresaId, CandidateAttemptEntity attempt) {
        delegate.deliverResultReady(empresaId, attempt);

        EmpresaIntegrationEntity integration = integrationRepository
                .findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .orElse(null);
        if (integration == null || integration.getStatus() != IntegrationStatus.CONECTADA) {
            return;
        }

        String lastError = readLastError(integration.getSettingsJson());
        if (lastError != null && !lastError.isBlank()) {
            throw new CustomWebhookDeliveryException(lastError);
        }
    }

    private String readLastError(String settingsJson) {
        if (settingsJson == null || settingsJson.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(settingsJson).get("lastError");
            return node == null || node.isNull() ? null : node.asText();
        } catch (Exception exception) {
            throw new CustomWebhookDeliveryException("Não foi possível confirmar a entrega CUSTOM_API.", exception);
        }
    }

    public static class CustomWebhookDeliveryException extends RuntimeException {
        public CustomWebhookDeliveryException(String message) {
            super(message);
        }

        public CustomWebhookDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
