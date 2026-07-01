package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.billing.config.MercadoPagoProperties;
import br.com.iforce.praxis.billing.persistence.entity.SubscriptionPlanEntity;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    public JsonNode createSplitPreference(
            String title,
            long priceCents,
            long marketplaceFeeCents,
            String externalReference,
            Map<String, Object> metadata,
            String sellerAccessToken
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("title", title);
        item.put("quantity", 1);
        item.put("currency_id", "BRL");
        item.put("unit_price", reais(priceCents));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", List.of(item));
        body.put("external_reference", externalReference);
        body.put("marketplace", properties.marketplaceId());
        body.put("marketplace_fee", reais(marketplaceFeeCents));
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

        return post("/checkout/preferences", body, sellerAccessToken);
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

    public JsonNode exchangeAuthorizationCode(String code) {
        requireEnabled();
        if (isBlank(properties.clientId()) || isBlank(properties.clientSecret()) || isBlank(properties.connectRedirectUri())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OAuth Mercado Pago Connect nao esta configurado."
            );
        }
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", properties.clientId());
        body.add("client_secret", properties.clientSecret());
        body.add("code", code);
        body.add("redirect_uri", properties.connectRedirectUri());

        try {
            return restClient.post()
                    .uri("/oauth/token")
                    .header(HttpHeaders.CONTENT_TYPE, "application/x-www-form-urlencoded")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RuntimeException exception) {
            log.error("Falha ao trocar code Mercado Pago Connect por token.");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao conectar Mercado Pago.");
        }
    }

    public String connectAuthorizationUrl(String state) {
        if (isBlank(properties.clientId()) || isBlank(properties.connectRedirectUri())) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OAuth Mercado Pago Connect nao esta configurado."
            );
        }
        return properties.authorizationUrl()
                + "?response_type=code&client_id=" + encode(properties.clientId())
                + "&redirect_uri=" + encode(properties.connectRedirectUri())
                + "&state=" + encode(state);
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

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
