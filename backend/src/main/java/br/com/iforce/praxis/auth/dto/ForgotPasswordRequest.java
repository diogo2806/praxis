package br.com.iforce.praxis.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitação de recuperação de senha.
 *
 * <p>Usuários EMPRESA informam {@code tenantId} e {@code email}; o ADMIN informa apenas o
 * {@code email} (o tenant técnico PLATFORM é assumido quando {@code tenantId} vem em branco).</p>
 *
 * <p>A resposta da API é sempre idêntica, independentemente de o usuário existir ou não, para
 * nunca revelar a existência de contas, e-mails ou tenants.</p>
 */
public record ForgotPasswordRequest(
        String tenantId,

        @NotBlank
        @Email
        String email
) {
}
