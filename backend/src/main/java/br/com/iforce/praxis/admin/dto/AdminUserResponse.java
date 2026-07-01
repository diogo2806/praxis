package br.com.iforce.praxis.admin.dto;

import br.com.iforce.praxis.admin.model.UserStatus;


import java.time.Instant;

import java.util.Set;


/** Usuário de acesso de um cliente, exibido na aba Acessos. */
public record AdminUserResponse(
        Long id,
        String name,
        String email,
        Set<String> roles,
        UserStatus status,
        Instant lastLoginAt,
        Instant createdAt
) {
}
