package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.billing.config.MercadoPagoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Valida a assinatura {@code x-signature} dos webhooks do Mercado Pago.
 *
 * <p>O Mercado Pago envia {@code x-signature: ts=<timestamp>,v1=<hmac>} e o template do manifesto
 * é {@code id:<dataId>;request-id:<x-request-id>;ts:<ts>;}. O HMAC-SHA256 do manifesto, com o
 * {@code MP_WEBHOOK_SECRET}, deve bater com {@code v1}.</p>
 */
@Component
public class MercadoPagoSignatureValidator {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoSignatureValidator.class);

    private final MercadoPagoProperties properties;

    public MercadoPagoSignatureValidator(MercadoPagoProperties properties) {
        this.properties = properties;
    }

    /**
     * Valida a assinatura da notificação.
     *
     * @param xSignature cabeçalho {@code x-signature}
     * @param xRequestId cabeçalho {@code x-request-id}
     * @param dataId     id do recurso (query {@code data.id})
     * @return true quando a assinatura é válida (ou quando o segredo não está configurado, em
     *         ambiente de desenvolvimento, registrando aviso)
     */
    public boolean isValid(String xSignature, String xRequestId, String dataId) {
        String secret = properties.webhookSecret();
        if (secret == null || secret.isBlank()) {
            if (properties.enabled()) {
                log.error("MP_WEBHOOK_SECRET ausente com mp.enabled=true: rejeitando webhook por segurança.");
                return false;
            }
            log.warn("MP_WEBHOOK_SECRET ausente: assinatura do webhook não verificada (apenas dev).");
            return true;
        }
        if (xSignature == null || xSignature.isBlank()) {
            return false;
        }

        String ts = null;
        String v1 = null;
        for (String part : xSignature.split(",")) {
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            String key = kv[0].trim();
            String value = kv[1].trim();
            if ("ts".equals(key)) {
                ts = value;
            } else if ("v1".equals(key)) {
                v1 = value;
            }
        }
        if (ts == null || v1 == null) {
            return false;
        }

        String manifest = "id:" + (dataId == null ? "" : dataId)
                + ";request-id:" + (xRequestId == null ? "" : xRequestId)
                + ";ts:" + ts + ";";
        String expected = hmacSha256Hex(secret, manifest);
        return constantTimeEquals(expected, v1.toLowerCase());
    }

    private static String hmacSha256Hex(String secret, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao calcular assinatura HMAC.", exception);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
