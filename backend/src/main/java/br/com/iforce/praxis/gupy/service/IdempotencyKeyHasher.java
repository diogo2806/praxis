package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.shared.security.Sha256;

/**
 * Deriva a chave de idempotência da participação a partir de um hash estável.
 *
 * <p>A chave composta pode conter dado pessoal. Guardar essa composição em
 * claro em uma coluna indexada exporia o dado sem necessidade. O SHA-256 em
 * hexadecimal preserva a comparação determinística sem persistir a entrada.</p>
 *
 * <p>A migração {@code V68} usa a mesma representação hexadecimal. Mudanças de
 * formato precisam permanecer compatíveis com os dados existentes.</p>
 */
public final class IdempotencyKeyHasher {

    private IdempotencyKeyHasher() {
    }

    public static String sha256Hex(String rawKey) {
        String scopedHash = CandidateAttemptIdempotencyScope.resolve(rawKey);
        if (scopedHash != null) {
            return scopedHash;
        }
        return Sha256.hex(rawKey);
    }
}
