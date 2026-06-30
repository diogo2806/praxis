package br.com.iforce.praxis.auth.dto;

import java.time.Instant;

/**
 * Resposta da validação de um token de recuperação de senha.
 *
 * <p>Devolvida apenas quando o token é válido e não expirou, permitindo ao frontend exibir o
 * nome do usuário e o prazo de expiração antes de coletar a nova senha.</p>
 */
public record ResetPasswordTokenResponse(
        boolean valid,
        Instant expiresAt,
        String userName
) {
}
