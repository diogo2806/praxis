package br.com.iforce.praxis.shared.security;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/** Gera valores aleatórios URL-safe para credenciais e convites. */
public final class SecureTokens {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private SecureTokens() {
    }

    public static String randomUrlSafe(int byteCount) {
        if (byteCount <= 0) {
            throw new IllegalArgumentException("A quantidade de bytes do token deve ser maior que zero.");
        }
        byte[] bytes = new byte[byteCount];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    public static String prefixed(String prefix, int byteCount) {
        return Objects.requireNonNull(prefix, "Prefixo do token obrigatório.") + randomUrlSafe(byteCount);
    }
}
