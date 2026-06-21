package br.com.iforce.praxis.candidate.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BlindMaskingTest {

    @Test
    void derivesStableUppercaseShortCodeFromAttemptId() {
        assertThat(BlindMasking.shortCode("att_abc123def456")).isEqualTo("DEF456");
        // Estável: o mesmo attemptId sempre gera o mesmo código.
        assertThat(BlindMasking.shortCode("att_abc123def456")).isEqualTo("DEF456");
    }

    @Test
    void maskedNameHidesIdentityBehindCode() {
        assertThat(BlindMasking.maskedName("att_abc123def456")).isEqualTo("Candidato DEF456");
    }

    @Test
    void handlesShortAndNonAlphanumericIds() {
        assertThat(BlindMasking.shortCode("a1b2")).isEqualTo("A1B2");
        assertThat(BlindMasking.shortCode("----")).isEqualTo("----");
    }

    @Test
    void handlesNullAndBlank() {
        assertThat(BlindMasking.shortCode(null)).isEqualTo("—");
        assertThat(BlindMasking.shortCode("   ")).isEqualTo("—");
    }

    @Test
    void differentAttemptsProduceDifferentCodes() {
        assertThat(BlindMasking.shortCode("att_aaa111"))
                .isNotEqualTo(BlindMasking.shortCode("att_bbb222"));
    }
}
