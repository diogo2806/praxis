package br.com.iforce.praxis.auth.dto;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.Size;


public record AcceptInviteRequest(
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