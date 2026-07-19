package br.com.iforce.praxis.shared.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecureTokensTest {

    @Test
    void shouldGenerateDistinctUrlSafeTokensWithPrefix() {
        String first = SecureTokens.prefixed("prx_", 32);
        String second = SecureTokens.prefixed("prx_", 32);

        assertThat(first).startsWith("prx_");
        assertThat(first.substring(4)).matches("[A-Za-z0-9_-]{43}");
        assertThat(second).isNotEqualTo(first);
    }

    @Test
    void shouldRejectNonPositiveTokenSize() {
        assertThatThrownBy(() -> SecureTokens.randomUrlSafe(0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
