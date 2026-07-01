package br.com.iforce.praxis.shared.integration.service;

import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;


class HmacSignatureServiceTest {

    private final HmacSignatureService service = new HmacSignatureService();

    @Test
    void matchesKnownHmacSha256Vector() {
        // Vetor de teste conhecido (RFC-style): key="key", message="The quick brown fox jumps over the lazy dog".
        String signature = service.sign("The quick brown fox jumps over the lazy dog", "key");

        assertThat(signature)
                .isEqualTo("sha256=f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }

    @Test
    void isDeterministicAndPrefixed() {
        String first = service.sign("{\"event\":\"RESULT_READY\"}", "whsec_abc");
        String second = service.sign("{\"event\":\"RESULT_READY\"}", "whsec_abc");

        assertThat(first).isEqualTo(second);
        assertThat(first).startsWith("sha256=");
    }

    @Test
    void differsWhenSecretChanges() {
        String a = service.sign("payload", "secret-a");
        String b = service.sign("payload", "secret-b");

        assertThat(a).isNotEqualTo(b);
    }
}
