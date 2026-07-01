package br.com.iforce.praxis.admin.dto;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import jakarta.validation.constraints.NotBlank;


/**
 * Reativação de um cliente. Exige motivo obrigatório e permite escolher o status alvo
 * ({@code ATIVO} ou {@code EM_TESTE}); assume {@code ATIVO} quando ausente.
 */
public record ReactivateEmpresaAdminRequest(
        @NotBlank String reason,
        EmpresaStatus targetStatus
) {
}
