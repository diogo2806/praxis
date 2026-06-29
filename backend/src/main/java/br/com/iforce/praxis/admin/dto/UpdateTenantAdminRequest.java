package br.com.iforce.praxis.admin.dto;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

/**
 * Alteração parcial dos dados cadastrais e comerciais de um cliente.
 *
 * <p>Campos nulos são ignorados (semântica de PATCH). Mudanças de plano e de condição
 * comercial geram eventos de auditoria específicos.</p>
 */
public record UpdateTenantAdminRequest(
        String name,
        String tradeName,
        String legalName,
        String taxId,
        String corporateEmail,
        String phone,
        String website,
        Boolean healthVertical,
        CommercialPlanType commercialPlanType,
        String commercialCondition
) {
}
