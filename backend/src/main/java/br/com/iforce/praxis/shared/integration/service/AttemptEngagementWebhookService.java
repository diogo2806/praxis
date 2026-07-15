package br.com.iforce.praxis.shared.integration.service;

import br.com.iforce.praxis.gupy.delivery.service.GupyOutboundUrlValidator;
import br.com.iforce.praxis.shared.integration.IntegrationManagementService;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;
import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.stream.StreamSupport;

/**
 * Entrega eventos proprietários de engajamento exclusivamente ao webhook
 * genérico CUSTOM_API configurado pela empresa.
 *
 * <p>Este contrato é deliberadamente separado do {@code result_webhook_url}
 * recebido da Gupy, que permanece reservado ao payload contratual
 * {@code TestResult}.</p>
 */
@Slf4j
@Service
public class AttemptEngagementWebhookService {

    public static final String ATTEMPT_STARTED_EVENT = "ATTEMPT_STARTED";
    public static final String ATTEMPT_ABANDONED_EVENT = "ATTEMPT_ABANDONED";

    private static final int ERROR_MESSAGE_LIMIT = 500;

    private final EmpresaIntegrationRepository integrationRepository;
    private final GupyOutboundUrlValidator outboundUrlValidator;
    private final HmacSignatureService hmacSignatureService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final IntegrationManagementService integrationManagementService;

    public AttemptEngagementWebhookService(
            EmpresaIntegrationRepository integrationRepository,
            GupyOutboundUrlValidator outboundUrlValidator,
            HmacSignatureService hmacSignatureService,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            IntegrationManagementService integrationManagementService
    ) {
        this.integrationRepository = integrationRepository;
        this.outboundUrlValidator = outboundUrlValidator;
        this.hmacSignatureService = hmacSignatureService;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
        this.integrationManagementService = integrationManagementService;
    }

    /** Retorna verdadeiro somente quando o evento foi selecionado no webhook CUSTOM_API ativo. */
    public boolean hasActiveWebhook(String empresaId, String eventType) {
        return findActiveSettings(empresaId, eventType) != null;
    }

    /**
     * Envia um evento de engajamento usando a URL e o segredo da integração
     * CUSTOM_API. A URL da Gupy nunca participa deste fluxo.
     */
    public void deliver(String empresaId, String eventType, JsonNode payload) {
        ActiveSettings activeSettings = findActiveSettings(empresaId, eventType);
        if (activeSettings == null) {
            return;
        }
        if (!eventType.equals(payload.path("event").asText())) {
            throw new AttemptEngagementWebhookDeliveryException(
                    "O payload do evento de engajamento não corresponde ao tipo do outbox."
            );
        }

        try {
            URI destination = outboundUrlValidator.validate(activeSettings.webhookUrl());
            String body = objectMapper.writeValueAsString(payload);
            String signature = hmacSignatureService.sign(body, activeSettings.secret());

            restClient.post()
                    .uri(destination)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HmacSignatureService.SIGNATURE_HEADER, signature)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            recordDelivery(activeSettings.integration(), true, null);
            recordActivityBestEffort(empresaId);
        } catch (Exception exception) {
            recordDelivery(activeSettings.integration(), false, exception.getMessage());
            throw new AttemptEngagementWebhookDeliveryException(
                    "Falha ao entregar evento de engajamento ao webhook CUSTOM_API.",
                    exception
            );
        }
    }

    private ActiveSettings findActiveSettings(String empresaId, String eventType) {
        EmpresaIntegrationEntity integration = integrationRepository
                .findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .filter(entity -> entity.getStatus() == IntegrationStatus.CONECTADA)
                .orElse(null);
        if (integration == null) {
            return null;
        }

        ObjectNode settings = readSettings(integration);
        String webhookUrl = textOrNull(settings.get("webhookUrl"));
        String secret = textOrNull(settings.get("secret"));
        boolean enabled = settings.path("events").isArray()
                && StreamSupport.stream(settings.path("events").spliterator(), false)
                .map(JsonNode::asText)
                .anyMatch(eventType::equals);
        if (webhookUrl == null || webhookUrl.isBlank() || secret == null || secret.isBlank() || !enabled) {
            return null;
        }
        return new ActiveSettings(integration, webhookUrl, secret);
    }

    private ObjectNode readSettings(EmpresaIntegrationEntity integration) {
        String settingsJson = integration.getSettingsJson();
        if (settingsJson == null || settingsJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        try {
            JsonNode settings = objectMapper.readTree(settingsJson);
            return settings instanceof ObjectNode objectNode ? objectNode : objectMapper.createObjectNode();
        } catch (Exception exception) {
            log.warn("Configuração CUSTOM_API inválida para a empresa {}", integration.getEmpresaId());
            return objectMapper.createObjectNode();
        }
    }

    private void recordDelivery(EmpresaIntegrationEntity integration, boolean delivered, String errorMessage) {
        ObjectNode settings = readSettings(integration);
        if (delivered) {
            settings.put("lastDeliveryAt", Instant.now().toString());
            settings.putNull("lastError");
        } else {
            settings.put("lastError", limit(errorMessage));
        }
        integration.setSettingsJson(settings.toString());
        integration.setUpdatedAt(Instant.now());
        integrationRepository.save(integration);
    }

    private void recordActivityBestEffort(String empresaId) {
        try {
            integrationManagementService.recordActivity(empresaId, IntegrationProvider.CUSTOM_API);
        } catch (Exception exception) {
            log.warn("Falha ao registrar atividade CUSTOM_API após entrega: {}", exception.getMessage());
        }
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private String limit(String value) {
        if (value == null || value.isBlank()) {
            return "Falha não detalhada na entrega do webhook.";
        }
        return value.length() <= ERROR_MESSAGE_LIMIT ? value : value.substring(0, ERROR_MESSAGE_LIMIT);
    }

    private record ActiveSettings(
            EmpresaIntegrationEntity integration,
            String webhookUrl,
            String secret
    ) {
    }

    public static class AttemptEngagementWebhookDeliveryException extends RuntimeException {
        public AttemptEngagementWebhookDeliveryException(String message) {
            super(message);
        }

        public AttemptEngagementWebhookDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
