package br.com.iforce.praxis.marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterProfessionalRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Email @Size(max = 180) String email,
        @NotBlank @Size(min = 8, max = 120) String password,
        @NotBlank @Size(max = 20) String document,
        @Size(max = 50) String professionalRegistration,
        @Size(max = 4000) String bio,
        Set<@Size(max = 60) String> specialties,
        @Size(max = 300) String linkedinUrl,
        @NotBlank @Size(max = 300) String lattesUrl,
        @Size(max = 150) String pixKey
) {
}
