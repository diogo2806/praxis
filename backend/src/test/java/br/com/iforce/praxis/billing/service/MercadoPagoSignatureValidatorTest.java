package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.billing.config.MercadoPagoProperties;

import org.junit.jupiter.api.Test;


import javax.crypto.Mac;

import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import java.util.HexFormat;


import static org.assertj.core.api.Assertions.assertThat;


class MercadoPagoSignatureValidatorTest {

    private static final String SECRET = "webhook-secret-de-teste";

    private MercadoPagoProperties properties(String secret) {
        return new MercadoPagoProperties(
                true,
                null,
                "token",
                "pub",
                secret,
                7,
                null,
                null
        );
    }

    private String signature(String ts, String dataId, String requestId) throws Exception {
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hash = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
        return "ts=" + ts + ",v1=" + hash;
    }

    @Test
    void acceptsValidSignature() throws Exception {
        var validator = new MercadoPagoSignatureValidator(properties(SECRET));
        String header = signature("1700000000", "123456", "req-1");
        assertThat(validator.isValid(header, "req-1", "123456")).isTrue();
    }

    @Test
    void rejectsTamperedSignature() {
        var validator = new MercadoPagoSignatureValidator(properties(SECRET));
        assertThat(validator.isValid("ts=1700000000,v1=deadbeef", "req-1", "123456")).isFalse();
    }

    @Test
    void rejectsWhenHeaderMissingButSecretConfigured() {
        var validator = new MercadoPagoSignatureValidator(properties(SECRET));
        assertThat(validator.isValid(null, "req-1", "123456")).isFalse();
    }

    @Test
    void acceptsInDevWhenSecretBlankAndMpDisabled() {
        var validator = new MercadoPagoSignatureValidator(new MercadoPagoProperties(
                false,
                null,
                "token",
                "pub",
                "",
                7,
                null,
                null
        ));
        assertThat(validator.isValid(null, null, null)).isTrue();
    }
}
