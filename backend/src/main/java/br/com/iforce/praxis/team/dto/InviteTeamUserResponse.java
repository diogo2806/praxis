package br.com.iforce.praxis.team.dto;

public record InviteTeamUserResponse(
        TeamUserResponse user,
        String inviteUrl
) {
}
