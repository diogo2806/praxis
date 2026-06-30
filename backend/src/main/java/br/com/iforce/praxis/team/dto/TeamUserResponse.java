package br.com.iforce.praxis.team.dto;

import br.com.iforce.praxis.admin.model.UserStatus;

import java.time.Instant;
import java.util.Set;

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
