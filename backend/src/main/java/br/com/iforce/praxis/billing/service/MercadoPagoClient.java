package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.billing.config.MercadoPagoProperties;
import br.com.iforce.praxis.billing.persistence.entity.SubscriptionPlanEntity;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Ponte de comunicação segura entre o backend e o Mercado Pago. */
@Component
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);

    private final MercadoPagoProperties properties;
    private final RestClient restClient;

    public MercadoPagoClient(MercadoPagoProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    /** Cria uma preferência de checkout de pagamento único para créditos avulsos. */
    public JsonNode createCreditPreference(SubscriptionPlanEntity plan, String externalReference,
                                           Map<String, Object> metadata) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", plan.getName());
        item.put("quantity", 1);
        item.put("currency_id", plan.getCurrency());
        item.put("unit_price", reais(plan.getPriceCents()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(item));
        body.put("external_reference", externalReference);
        if (notBlank(properties.notificationUrl())) {
            body.put("notification_url", properties.notificationUrl());
        }
        if (notBlank(properties.backUrl())) {
            body.put("back_urls", Map.of(
                    "success", properties.backUrl(),
                    "failure", properties.backUrl(),
                    "pending", properties.backUrl()));
        }
        body.put("metadata", metadata);
        return post("/checkout/preferences", body);
    }

    /** Cria uma assinatura mensal recorrente para o plano PROFISSIONAL. */
    public JsonNode createPreapproval(SubscriptionPlanEntity plan, String payerEmail,
                                      String externalReference) {
        Map<String, Object> autoRecurring = new LinkedHashMap<>();
        autoRecurring.put("frequency", 1);
        autoRecurring.put("frequency_type", "months");
        autoRecurring.put("transaction_amount", reais(plan.getPriceCents()));
        autoRecurring.put("currency_id", plan.getCurrency());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", plan.getName());
        body.put("external_reference", externalReference);
        if (notBlank(payerEmail)) {
            body.put("payer_email", payerEmail);
        }
        body.put("auto_recurring", autoRecurring);
        if (notBlank(properties.backUrl())) {
            body.put("back_url", properties.backUrl());
        }
        if (notBlank(properties.notificationUrl())) {
            body.put("notification_url", properties.notificationUrl());
        }
        body.put("status", "pending");
        return post("/preapproval", body);
    }

    /** Cancela uma autorização de cobrança recorrente já existente. */
    public JsonNode cancelPreapproval(String preapprovalId) {
        if (!notBlank(preapprovalId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador da assinatura não informado.");
        }
        return put("/preapproval/" + preapprovalId, Map.of("status", "cancelled"));
    }

    /** Cobra um cartão salvo para uma recarga automática de créditos. */
    public JsonNode chargeSavedCard(SubscriptionPlanEntity plan, String customerId, String cardId,
                                    String externalReference, Map<String, Object> metadata,
                                    String idempotencyKey) {
        JsonNode token = post("/v1/card_tokens", Map.of("card_id", cardId));
        String cardToken = text(token, "id");

        Map<String, Object> payer = new LinkedHashMap<>();
        payer.put("type", "customer");
        payer.put("id", customerId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("transaction_amount", reais(plan.getPriceCents()));
        body.put("description", plan.getName());
        body.put("external_reference", externalReference);
        body.put("installments", 1);
        body.put("token", cardToken);
        body.put("payer", payer);
        if (notBlank(properties.notificationUrl())) {
            body.put("notification_url", properties.notificationUrl());
        }
        body.put("metadata", metadata);
        return post("/v1/payments", body, properties.accessToken(), idempotencyKey);
    }

    /** Consulta um pagamento no Mercado Pago. */
    public JsonNode getPayment(String paymentId) {
        return get("/v1/payments/" + paymentId);
    }

    /** Consulta uma assinatura no Mercado Pago. */
    public JsonNode getPreapproval(String preapprovalId) {
        return get("/preapproval/" + preapprovalId);
    }

    private JsonNode post(String path, Object body) {
        return post(path, body, properties.accessToken(), null);
    }

    private JsonNode post(String path, Object body, String accessToken) {
        return post(path, body, accessToken, null);
    }

    private JsonNode post(String path, Object body, String accessToken, String idempotencyKey) {
        requireEnabled();
        if (!notBlank(accessToken)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Token Mercado Pago do vendedor não está configurado.");
        }
        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            if (notBlank(idempotencyKey)) {
                request = request.header("X-Idempotency-Key", idempotencyKey);
            }
            return request.body(body).retrieve().body(JsonNode.class);
        } catch (RuntimeException exception) {
            log.error("Falha ao chamar Mercado Pago (POST {}). Token {}.", path, properties.maskedAccessToken());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha na comunicação com o Mercado Pago.");
        }
    }

    private JsonNode put(String path, Object body) {
        requireEnabled();
        try {
            return restClient.put()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException exception) {
            log.error("Falha ao chamar Mercado Pago (PUT {}). Token {}.", path, properties.maskedAccessToken());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha na comunicação com o Mercado Pago.");
        }
    }

    private JsonNode get(String path) {
        requireEnabled();
        try {
            return restClient.get()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException exception) {
            log.error("Falha ao consultar Mercado Pago (GET {}). Token {}.", path, properties.maskedAccessToken());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Falha na comunicação com o Mercado Pago.");
        }
    }

    private void requireEnabled() {
        if (!properties.enabled() || !notBlank(properties.accessToken())) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Integração Mercado Pago não está habilitada.");
        }
    }

    private static BigDecimal reais(long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
