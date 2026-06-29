package br.com.iforce.praxis.admin.dto;

/**
 * Resultado do cadastro de um cliente: o detalhe do tenant recém-criado, o ID do usuário
 * responsável e, quando o convite por link foi solicitado, a URL de convite gerada.
 */
public record CreateTenantAdminResponse(
        TenantAdminDetailResponse tenant,
        Long responsibleUserId,
        String inviteUrl
) {
}
