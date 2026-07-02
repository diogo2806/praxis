package br.com.iforce.praxis.marketplace.dto;

import jakarta.validation.constraints.Size;

import java.util.Set;

public record UpdateProfessionalProfileRequest(
        @Size(max = 150) String displayName,
        @Size(max = 4000) String bio,
        Set<@Size(max = 60) String> specialties,
        @Size(max = 300) String linkedinUrl,
        @Size(max = 300) String lattesUrl,
        @Size(max = 150) String pixKey
) {
}
