package br.com.iforce.praxis.shared.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Sha256Test {

    @Test
    void shouldKeepStableHexAndBase64UrlRepresentations() {
        assertThat(Sha256.hex("praxis"))
                .isEqualTo("d5007cc3f96992ad0c10be2fda2a18bc06d8fa1a5a1672e2f131682f47e28039");
        assertThat(Sha256.base64Url("praxis"))
                .isEqualTo("1QB8w_lpkq0MEL4v2ioYvAbY-hpaFnLi8TFoL0figDk");
        assertThat(Sha256.hexPrefix("praxis", 12))
                .isEqualTo("d5007cc3f96992ad0c10be2f");
    }

    @Test
    void shouldRejectInvalidPrefixLength() {
        assertThatThrownBy(() -> Sha256.hexPrefix("praxis", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sha256.hexPrefix("praxis", 33))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
