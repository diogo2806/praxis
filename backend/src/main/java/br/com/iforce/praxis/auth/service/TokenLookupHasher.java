package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.shared.security.Sha256;

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
        return Sha256.hex(token);
    }
}
