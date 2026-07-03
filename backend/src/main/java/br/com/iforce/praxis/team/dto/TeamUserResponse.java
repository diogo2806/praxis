package br.com.iforce.praxis.team.dto;

import br.com.iforce.praxis.admin.model.UserStatus;

import java.time.Instant;
import java.util.Set;

/**
 * Dados de um usuário da equipe exibidos na tela, na visão do cliente.
 *
 * @param id identificador do usuário
 * @param name nome da pessoa
 * @param email e-mail usado para login e para receber o convite
 * @param roles perfis de acesso do usuário
 * @param status situação atual: ativo, convidado ou bloqueado
 * @param lastLoginAt data e hora do último acesso (fica em branco enquanto a pessoa nunca entrou)
 * @param createdAt data e hora em que o usuário foi cadastrado
 */
public record TeamUserResponse(
        Long id,
        String name,
        String email,
        Set<String> roles,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt
) {
}
