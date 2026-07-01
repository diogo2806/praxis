package br.com.iforce.praxis.account.dto;

import java.util.Set;


public record AccountResponse(
        Long id,
        String empresaId,
        String name,
        String email,
        Set<String> roles
) {
}
