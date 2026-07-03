package br.com.iforce.praxis.admin.dto;

import jakarta.validation.constraints.Max;

import jakarta.validation.constraints.Positive;

import jakarta.validation.constraints.Size;


/**
 * Concessão de créditos de cortesia a um cliente (empresa) pelo operador ADMIN, para liberar
 * testes. A quantidade é positiva e limitada; a nota é opcional e fica registrada na auditoria.
 */
public record GrantCreditsAdminRequest(
        @Positive @Max(100000) int amount,
        @Size(max = 500) String note
) {
}
