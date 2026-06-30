package br.com.iforce.praxis.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Confirmação da redefinição de senha a partir do token recebido por e-mail.
 *
 * <p>A senha deve ter no mínimo 8 caracteres e não pode ser igual à atual. A confirmação é
 * validada no servidor mesmo já tendo sido checada no frontend.</p>
 */
public record ResetPasswordRequest(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 120)
        String newPassword,

        @NotBlank
        @Size(min = 8, max = 120)
        String confirmPassword
) {
}
