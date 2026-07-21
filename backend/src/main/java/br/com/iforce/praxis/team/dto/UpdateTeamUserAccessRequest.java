package br.com.iforce.praxis.team.dto;

import br.com.iforce.praxis.team.model.TeamProfile;
import jakarta.validation.constraints.NotNull;

/** Perfil que deve substituir o acesso atual de um usuário da equipe. */
public record UpdateTeamUserAccessRequest(
        @NotNull TeamProfile profile
) {
}
