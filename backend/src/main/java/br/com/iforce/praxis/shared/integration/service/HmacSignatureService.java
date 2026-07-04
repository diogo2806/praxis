package br.com.iforce.praxis.shared.integration.service;

import org.springframework.stereotype.Service;


import javax.crypto.Mac;

import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import java.security.GeneralSecurityException;


/**
 * Gera a assinatura de segurança enviada junto com cada webhook.
 *
 * <p>Na visão do processo, o cliente recebe eventos da Práxis em uma URL
 * própria. Para que ele consiga confirmar que a mensagem veio mesmo da Práxis e
 * que o conteúdo não foi alterado no caminho, cada envio recebe uma assinatura
 * calculada com o segredo configurado para aquele cliente.</p>
 *
 * <p>O cliente valida essa assinatura recomputando o HMAC-SHA256 sobre o corpo
 * recebido e comparando com o cabeçalho {@code X-Praxis-Signature}. O formato
 * enviado é {@code sha256=<hex>}.</p>
 */
@Service
public class HmacSignatureService {

    private static final String ALGORITHM = "HmacSHA256";

    /**
     * Nome do cabeçalho HTTP onde a Práxis coloca a assinatura do webhook.
     *
     * <p>Na prática, é o campo que o sistema do cliente deve ler para validar se
     * o evento recebido é confiável.</p>
     */
    public static final String SIGNATURE_HEADER = "X-Praxis-Signature";

    /**
     * Assina o conteúdo que será entregue ao webhook do cliente.
     *
     * <p>Fluxo do processo: antes de enviar o evento, a Práxis pega exatamente o
     * mesmo corpo JSON que será transmitido, combina com o segredo do cliente e
     * gera uma assinatura. Se o cliente calcular a mesma assinatura do lado dele,
     * significa que recebeu o conteúdo esperado.</p>
     *
     * @param payload corpo exato da mensagem que será enviada ao cliente
     * @param secret segredo compartilhado com o cliente para validar os webhooks
     * @return assinatura no formato esperado pelo cabeçalho {@code X-Praxis-Signature}
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

    /**
     * Transforma os bytes da assinatura em texto hexadecimal para transporte no
     * cabeçalho HTTP.
     *
     * <p>Uso interno: essa conversão torna a assinatura legível e compatível com
     * a forma mais comum de validação de webhooks.</p>
     *
     * @param bytes assinatura calculada em formato binário
     * @return assinatura em caracteres hexadecimais
     */
    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(Character.forDigit((b >> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }
}
