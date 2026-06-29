package br.com.iforce.praxis.admin.dto;

/** Resultado de um convite de usuário: o usuário criado/atualizado e a URL de convite. */
public record InviteUserAdminResponse(
        AdminUserResponse user,
        String inviteUrl
) {
}
