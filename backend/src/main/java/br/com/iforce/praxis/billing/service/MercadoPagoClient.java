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
 * Ponte de comunicação com o Mercado Pago — quem realmente "conversa" com a operadora de pagamento.
 *
 * <p>Na visão do processo, sempre que a plataforma precisa criar uma cobrança ou conferir um
 * pagamento, é este componente que faz a chamada pela internet até o Mercado Pago e traz a
 * resposta. Ele concentra os quatro diálogos possíveis: abrir a compra de créditos, abrir a
 * assinatura mensal, consultar um pagamento e consultar uma assinatura.</p>
 *
 * <p>Cuidados de segurança embutidos: a credencial de acesso (Access Token) vive apenas no backend,
 * nunca chega ao navegador, e aparece mascarada nos registros de log. Quando a integração está
 * desligada ({@code mp.enabled=false}) ou sem credencial, as chamadas que dependem de internet
 * falham de forma explícita, em vez de fingir sucesso.</p>
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

    /** Envia um pedido de escrita ao Mercado Pago usando a credencial padrão do vendedor. Uso interno. */
    private JsonNode post(String path, Object body) {
        return post(path, body, properties.accessToken());
    }

    /**
     * Faz a chamada de escrita ao Mercado Pago com a credencial informada, tratando falhas de rede
     * como um erro claro de comunicação (sem vazar a credencial nos logs). Uso interno.
     */
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

    /**
     * Faz uma consulta de leitura ao Mercado Pago (por exemplo, o estado de um pagamento), tratando
     * falhas de rede como um erro claro de comunicação. Uso interno.
     */
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

    /**
     * Garante que a integração está ligada e com credencial antes de qualquer chamada; caso
     * contrário, interrompe com um aviso claro de que o Mercado Pago não está habilitado. Uso interno.
     */
    private void requireEnabled() {
        if (!properties.enabled() || properties.accessToken() == null || properties.accessToken().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Integração Mercado Pago não está habilitada.");
        }
    }

    /** Converte o valor guardado em centavos para reais, no formato que o Mercado Pago espera. Uso interno. */
    private static BigDecimal reais(long cents) {
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /** Diz se um texto tem conteúdo (não é nulo nem vazio), usado para só enviar campos preenchidos. Uso interno. */
    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
