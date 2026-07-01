package br.com.iforce.praxis.team.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteTeamUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email
) {
}
