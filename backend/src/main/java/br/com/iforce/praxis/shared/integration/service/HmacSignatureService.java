package br.com.iforce.praxis.shared.integration.service;

import org.springframework.stereotype.Service;


import javax.crypto.Mac;

import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import java.security.GeneralSecurityException;


/**
 * Assina o corpo das requisições de webhook com HMAC-SHA256, usando o segredo
 * do cliente (tenant). O cliente valida a assinatura recebida no cabeçalho
 * {@code X-Praxis-Signature} recomputando o HMAC sobre o corpo recebido.
 *
 * <p>Formato da assinatura: {@code sha256=<hex>}.</p>
 */
@Service
public class HmacSignatureService {

    private static final String ALGORITHM = "HmacSHA256";

    /** Nome do cabeçalho HTTP que carrega a assinatura. */
    public static final String SIGNATURE_HEADER = "X-Praxis-Signature";

    /**
     * Calcula {@code sha256=<hex>} para o corpo informado com o segredo do cliente.
     *
     * @param payload corpo exato que será enviado (mesmos bytes)
     * @param secret segredo HMAC do cliente
     * @return a assinatura no formato {@code sha256=<hex>}
     */
    public String sign(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] raw = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return "sha256=" + toHex(raw);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Não foi possível assinar o payload do webhook.", exception);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }
}
