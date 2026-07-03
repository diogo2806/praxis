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

/**
 * Cliente HTTP do Mercado Pago. O Access Token vive apenas no backend (nunca no frontend) e é
 * mascarado em logs. Quando a integração está desabilitada ({@code mp.enabled=false}), as chamadas
 * que exigem rede falham de forma explícita.
 */
@Component
public class MercadoPagoClient {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoClient.class);

    private final MercadoPagoProperties properties;
    private final RestClient restClient;

    public MercadoPagoClient(MercadoPagoProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.baseUrl(properties.baseUrl()).build();
    }

    /** Cria uma preferência de checkout (pagamento único) para compra de créditos AVULSO. */
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

    /** Cria uma assinatura recorrente (preapproval) para o plano PROFISSIONAL. */
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

    /** Consulta um pagamento no Mercado Pago (fonte da verdade financeira). */
    public JsonNode getPayment(String paymentId) {
        return get("/v1/payments/" + paymentId);
    }

    /** Consulta uma assinatura (preapproval) no Mercado Pago. */
    public JsonNode getPreapproval(String preapprovalId) {
        return get("/preapproval/" + preapprovalId);
    }

    private JsonNode post(String path, Object body) {
        return post(path, body, properties.accessToken());
    }

    private JsonNode post(String path, Object body, String accessToken) {
        requireEnabled();
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Token Mercado Pago do vendedor nao esta configurado.");
        }
        try {
            return restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException exception) {
            log.error("Falha ao chamar Mercado Pago (POST {}). Token {}.", path, properties.maskedAccessToken());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha na comunicação com o Mercado Pago.");
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
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha na comunicação com o Mercado Pago.");
        }
    }

    private void requireEnabled() {
        if (!properties.enabled() || properties.accessToken() == null || properties.accessToken().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Integração Mercado Pago não está habilitada.");
        }
    }

    private static BigDecimal reais(long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
