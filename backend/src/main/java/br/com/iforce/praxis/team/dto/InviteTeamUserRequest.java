package br.com.iforce.praxis.team.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Dados informados pelo cliente para convidar alguém para a equipe.
 *
 * @param name nome da pessoa a convidar
 * @param email e-mail da pessoa, por onde o convite é enviado e que servirá de login
 */
public record InviteTeamUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email
) {
}
