package br.com.iforce.praxis.gupy.service;

import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;


class IdempotencyKeyHasherTest {

    @Test
    void matchesKnownSha256HexVector() {
        // Vetor canônico NIST: SHA-256("abc").
        assertThat(IdempotencyKeyHasher.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    void isDeterministicForTheSameInput() {
        String raw = "empresa-1|empresa-123|12345678900|sim-atendimento";

        assertThat(IdempotencyKeyHasher.sha256Hex(raw))
                .isEqualTo(IdempotencyKeyHasher.sha256Hex(raw));
    }

    @Test
    void doesNotLeakPersonalDataFromTheRawKey() {
        String cpf = "12345678900";
        String email = "candidato@example.com";

        String hashed = IdempotencyKeyHasher.sha256Hex("empresa-1|empresa-123|" + cpf + "|sim");
        String hashedEmail = IdempotencyKeyHasher.sha256Hex("empresa-1|company|" + email + "|sim");

        assertThat(hashed).doesNotContain(cpf).hasSize(64).containsPattern("^[0-9a-f]+$");
        assertThat(hashedEmail).doesNotContain(email).hasSize(64);
    }

    @Test
    void differentInputsProduceDifferentHashes() {
        assertThat(IdempotencyKeyHasher.sha256Hex("empresa-1|c|doc-a|sim"))
                .isNotEqualTo(IdempotencyKeyHasher.sha256Hex("empresa-1|c|doc-b|sim"));
    }
}
