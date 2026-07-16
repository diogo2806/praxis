package br.com.iforce.praxis.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Calcula hashes determinísticos usados somente para localizar tokens aleatórios
 * antes da verificação criptográfica definitiva.
 */
public final class TokenLookupHasher {

    private TokenLookupHasher() {
    }

    public static String sha256(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token obrigatório para cálculo do hash de localização.");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
