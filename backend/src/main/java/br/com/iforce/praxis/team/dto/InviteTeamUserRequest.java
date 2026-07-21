package br.com.iforce.praxis.team.dto;

import br.com.iforce.praxis.team.model.TeamProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Dados informados pelo cliente para convidar alguém para a equipe.
 *
 * @param name nome da pessoa a convidar
 * @param email e-mail da pessoa, por onde o convite é enviado e que servirá de login
 * @param profile perfil operacional concedido ao aceitar o convite
 */
public record InviteTeamUserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        TeamProfile profile
) {

    public InviteTeamUserRequest(String name, String email) {
        this(name, email, TeamProfile.OPERADOR);
    }

    public TeamProfile resolvedProfile() {
        return profile == null ? TeamProfile.OPERADOR : profile;
    }
}
