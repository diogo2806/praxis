package br.com.iforce.praxis.billing.controller;

import br.com.iforce.praxis.billing.service.MercadoPagoWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Webhook público do Mercado Pago. Não exige JWT e está fora do {@code TenantResolutionFilter}.
 * A autenticidade é garantida pela validação da assinatura {@code x-signature}.
 */
@RestController
@RequestMapping("/api/webhooks/mercado-pago")
@Tag(name = "Webhooks · Mercado Pago", description = "Recebimento de notificações de cobrança.")
public class MercadoPagoWebhookController {

    private final MercadoPagoWebhookService webhookService;
    private final ObjectMapper objectMapper;

    public MercadoPagoWebhookController(MercadoPagoWebhookService webhookService, ObjectMapper objectMapper) {
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    @Operation(summary = "Recebe notificação do Mercado Pago")
    public ResponseEntity<Void> receive(
            @RequestBody(required = false) Map<String, Object> body,
            @RequestParam(name = "type", required = false) String typeParam,
            @RequestParam(name = "topic", required = false) String topicParam,
            @RequestParam(name = "id", required = false) String idParam,
            @RequestParam(name = "data.id", required = false) String dataIdParam,
            @RequestHeader(name = "x-signature", required = false) String xSignature,
            @RequestHeader(name = "x-request-id", required = false) String xRequestId
    ) {
        String topic = firstNonBlank(stringField(body, "type"), stringField(body, "topic"), typeParam, topicParam);
        String dataId = firstNonBlank(nestedDataId(body), dataIdParam, idParam, stringField(body, "id"));

        String rawPayload = body == null ? null : serialize(body);
        webhookService.handle(topic, dataId, xSignature, xRequestId, rawPayload);

        // Responde 200 quando recebido corretamente, conforme contrato do Mercado Pago.
        return ResponseEntity.ok().build();
    }

    private String serialize(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            return String.valueOf(body);
        }
    }

    @SuppressWarnings("unchecked")
    private String nestedDataId(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object data = body.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object id = ((Map<String, Object>) dataMap).get("id");
            return id == null ? null : String.valueOf(id);
        }
        return null;
    }

    private static String stringField(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
