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

    /**
     * Cobra um cartão já salvo do cliente para uma recarga automática de créditos (AVULSO).
     *
     * <p>É a cobrança "sem atrito humano": em vez de gerar um link para o cliente pagar, a
     * plataforma usa o cartão que ele já cadastrou no Mercado Pago (referenciado por
     * {@code customerId}/{@code cardId}) e faz a cobrança diretamente. O fluxo segue o padrão do
     * Mercado Pago para cartão salvo: primeiro gera um token a partir do cartão salvo e depois
     * cria o pagamento com esse token.</p>
     *
     * <p>A chave de idempotência ({@code idempotencyKey}) é enviada ao Mercado Pago para que, se a
     * mesma recarga for tentada de novo (por exemplo, após uma falha de rede), o cartão não seja
     * cobrado duas vezes: o Mercado Pago devolve o mesmo pagamento já criado.</p>
     *
     * @param plan pacote de créditos AVULSO que define o valor a cobrar
     * @param customerId identificador do cliente/pagador salvo no Mercado Pago
     * @param cardId identificador do cartão salvo no Mercado Pago
     * @param externalReference etiqueta que permite reconhecer o pagamento quando voltar
     * @param metadata dados extras para a trilha do Mercado Pago
     * @param idempotencyKey chave que evita cobrança dupla em retentativas da mesma recarga
     * @return o pagamento criado no Mercado Pago (com id e status)
     */
    public JsonNode chargeSavedCard(SubscriptionPlanEntity plan, String customerId, String cardId,
                                    String externalReference, Map<String, Object> metadata,
                                    String idempotencyKey) {
        // 1) Gera um token de uso único a partir do cartão já salvo do cliente.
        JsonNode token = post("/v1/card_tokens", Map.of("card_id", cardId));
        String cardToken = text(token, "id");

        // 2) Cria o pagamento cobrando o cartão salvo (uma parcela).
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
        return post(path, body, properties.accessToken(), null);
    }

    /** Escreve no Mercado Pago com a credencial informada, sem chave de idempotência. Uso interno. */
    private JsonNode post(String path, Object body, String accessToken) {
        return post(path, body, accessToken, null);
    }

    /**
     * Faz a chamada de escrita ao Mercado Pago com a credencial informada, tratando falhas de rede
     * como um erro claro de comunicação (sem vazar a credencial nos logs). Quando uma chave de
     * idempotência é informada, ela vai no cabeçalho {@code X-Idempotency-Key} para que
     * retentativas do mesmo pedido não gerem cobrança dupla. Uso interno.
     */
    private JsonNode post(String path, Object body, String accessToken, String idempotencyKey) {
        requireEnabled();
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "Token Mercado Pago do vendedor nao esta configurado.");
        }
        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
            if (notBlank(idempotencyKey)) {
                request = request.header("X-Idempotency-Key", idempotencyKey);
            }
            return request
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

    /** Lê com segurança um campo de texto da resposta do Mercado Pago, tolerando ausências. Uso interno. */
    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
