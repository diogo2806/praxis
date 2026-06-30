package br.com.iforce.praxis.auth.dto;

import java.util.Set;


public record LoginResponse(
        String token,
        Long userId,
        String empresaId,
        String name,
        Set<String> roles
) {
}
