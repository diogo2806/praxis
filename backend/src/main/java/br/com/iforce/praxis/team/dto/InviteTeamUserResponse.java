package br.com.iforce.praxis.team.dto;

/**
 * Resposta de um convite: os dados do usuário recém-criado e o link que a
 * pessoa deve acessar para entrar pela primeira vez e definir a própria senha.
 *
 * @param user dados do usuário convidado
 * @param inviteUrl link de convite a ser enviado à pessoa
 */
public record InviteTeamUserResponse(
        TeamUserResponse user,
        String inviteUrl
) {
}
