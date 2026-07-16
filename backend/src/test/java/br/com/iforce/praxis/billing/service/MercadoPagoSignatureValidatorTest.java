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

    @Test
    void acceptsValidSignatureRegardlessOfHeaderOrderAndHexCase() throws Exception {
        MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(properties(true, SECRET));
        String timestamp = "1700000000";
        String hash = signatureHash(timestamp, "123456", "req-1").toUpperCase();
        String header = "v1=" + hash + ", ts=" + timestamp;

        assertThat(validator.isValid(header, "req-1", "123456")).isTrue();
    }

    @Test
    void rejectsTamperedSignature() {
        MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(properties(true, SECRET));

        assertThat(validator.isValid("ts=1700000000,v1=deadbeef", "req-1", "123456")).isFalse();
    }

    @Test
    void rejectsSignatureCalculatedForDifferentPayload() throws Exception {
        MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(properties(true, SECRET));
        String header = signature("1700000000", "123456", "req-1");

        assertThat(validator.isValid(header, "req-1", "654321")).isFalse();
        assertThat(validator.isValid(header, "req-2", "123456")).isFalse();
    }

    @Test
    void rejectsMissingOrIncompleteSignatureHeader() {
        MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(properties(true, SECRET));

        assertThat(validator.isValid(null, "req-1", "123456")).isFalse();
        assertThat(validator.isValid("", "req-1", "123456")).isFalse();
        assertThat(validator.isValid("ts=1700000000", "req-1", "123456")).isFalse();
        assertThat(validator.isValid("v1=abc", "req-1", "123456")).isFalse();
        assertThat(validator.isValid("invalid,ts=1700000000", "req-1", "123456")).isFalse();
    }

    @Test
    void acceptsValidManifestWithEmptyOptionalIdentifiers() throws Exception {
        MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(properties(true, SECRET));
        String header = signature("1700000000", "", "");

        assertThat(validator.isValid(header, null, null)).isTrue();
    }

    @Test
    void rejectsWhenSecretIsBlankAndMercadoPagoIsEnabled() {
        MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(properties(true, " "));

        assertThat(validator.isValid("ts=1700000000,v1=abc", "req-1", "123456")).isFalse();
    }

    @Test
    void acceptsUnsignedWebhookOnlyWhenMercadoPagoIsDisabled() {
        MercadoPagoSignatureValidator validator = new MercadoPagoSignatureValidator(properties(false, ""));

        assertThat(validator.isValid(null, null, null)).isTrue();
    }

    private MercadoPagoProperties properties(boolean enabled, String secret) {
        return new MercadoPagoProperties(
                enabled,
                null,
                "token",
                "pub",
                secret,
                7,
                null,
                null
        );
    }

    private String signature(String timestamp, String dataId, String requestId) throws Exception {
        return "v1=" + signatureHash(timestamp, dataId, requestId) + ", ts=" + timestamp;
    }

    private String signatureHash(String timestamp, String dataId, String requestId) throws Exception {
        String manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + timestamp + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)));
    }
}
