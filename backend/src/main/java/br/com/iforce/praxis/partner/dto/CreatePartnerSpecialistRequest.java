package br.com.iforce.praxis.partner.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePartnerSpecialistRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Email @Size(max = 180) String email
) {
}
