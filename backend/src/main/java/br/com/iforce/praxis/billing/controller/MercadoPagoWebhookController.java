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
 * Webhook público do Mercado Pago. Não exige JWT e está fora do {@code EmpresaResolutionFilter}.
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

    /**
     * Recebe a notificação enviada pelo Mercado Pago quando algo muda em um pagamento ou assinatura.
     *
     * <p>Na visão do processo, é a "campainha" que o Mercado Pago toca para avisar a plataforma.
     * Como o aviso pode chegar em formatos diferentes, este método primeiro descobre de que tipo de
     * evento se trata e a qual recurso ele se refere (olhando o corpo e os parâmetros da chamada), e
     * então entrega tudo — inclusive os cabeçalhos de assinatura e rastreio — para a regra de
     * negócio conferir a autenticidade e processar. Responde sempre {@code 200 OK} quando o
     * recebimento ocorre bem, como o contrato do Mercado Pago exige.</p>
     *
     * @param body corpo da notificação (pode vir vazio em avisos de teste)
     * @param typeParam tipo do evento informado por parâmetro de URL (alternativa ao corpo)
     * @param topicParam tópico do evento informado por parâmetro de URL (alternativa ao corpo)
     * @param idParam identificador do recurso informado por parâmetro de URL
     * @param dataIdParam identificador do recurso no parâmetro {@code data.id}
     * @param xSignature cabeçalho de assinatura usado para provar que o aviso veio mesmo do Mercado Pago
     * @param xRequestId cabeçalho de rastreio da requisição
     * @return {@code 200 OK} quando a notificação é recebida corretamente
     */
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

    /** Transforma o corpo recebido em texto para ser guardado como comprovante. Uso interno. */
    private String serialize(Map<String, Object> body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception exception) {
            return String.valueOf(body);
        }
    }

    /** Pesca o identificador do recurso que vem aninhado dentro de {@code data.id} no corpo. Uso interno. */
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

    /** Lê com segurança um campo de texto do corpo recebido, tolerando ausências. Uso interno. */
    private static String stringField(Map<String, Object> body, String key) {
        if (body == null) {
            return null;
        }
        Object value = body.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Escolhe o primeiro valor preenchido entre várias fontes possíveis, já que o Mercado Pago pode
     * mandar o mesmo dado ora no corpo, ora na URL. Uso interno.
     */
    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
