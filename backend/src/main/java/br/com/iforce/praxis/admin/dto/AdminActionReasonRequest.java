package br.com.iforce.praxis.admin.dto;

import jakarta.validation.constraints.NotBlank;


/**
 * Motivo obrigatório para ações administrativas sensíveis (suspender, cancelar).
 * O motivo é registrado na trilha de auditoria append-only.
 */
public record AdminActionReasonRequest(
        @NotBlank String reason
) {
}
