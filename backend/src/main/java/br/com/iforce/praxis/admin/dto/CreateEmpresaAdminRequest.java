package br.com.iforce.praxis.admin.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import jakarta.validation.constraints.Email;

import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.NotNull;


/**
 * Dados para provisionar um cliente completo a partir do painel ADMIN.
 *
 * <p>Ao salvar, o sistema cria o {@code EmpresaEntity}, preenche dados comerciais e cria o
 * primeiro usuário responsável com papel {@code EMPRESA} (nunca {@code ADMIN}).</p>
 */
public record CreateEmpresaAdminRequest(
        @NotBlank String name,
        String tradeName,
        String legalName,
        String taxId,
        @Email String corporateEmail,
        String phone,
        String website,
        boolean healthVertical,
        /** Opcional: identificador de integração; gerado quando ausente. */
        String companyId,
        @NotNull CommercialPlanType commercialPlanType,
        String commercialCondition,
        /** Status inicial do cliente; assume {@code EM_TESTE} quando ausente. */
        EmpresaStatus initialStatus,
        @NotBlank String responsibleName,
        @NotBlank @Email String responsibleEmail,
        boolean sendInvite
) {
}
