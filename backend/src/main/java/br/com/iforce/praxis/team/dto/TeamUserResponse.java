package br.com.iforce.praxis.team.dto;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.team.model.TeamProfile;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Dados de um usuário da equipe exibidos na tela, na visão do cliente.
 *
 * @param id identificador do usuário
 * @param name nome da pessoa
 * @param email e-mail usado para login e para receber o convite
 * @param roles papéis técnicos persistidos para autorização
 * @param profile perfil operacional apresentado à empresa
 * @param permissions permissões resumidas do perfil
 * @param status situação atual: ativo, convidado ou bloqueado
 * @param lastLoginAt data e hora do último acesso
 * @param createdAt data e hora em que o usuário foi cadastrado
 */
public record TeamUserResponse(
        Long id,
        String name,
        String email,
        Set<String> roles,
        TeamProfile profile,
        List<String> permissions,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt
) {
}
