package br.com.iforce.praxis.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        String tenantId,
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
