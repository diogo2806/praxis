package br.com.iforce.praxis.billing.dto;

import jakarta.validation.constraints.NotBlank;


/**
 * Sincronização manual de um recurso do Mercado Pago. Consulta a API do MP e aplica o efeito
 * financeiro correspondente — nunca marca pagamento como aprovado sem consultar.
 */
public record ManualSyncRequest(
        @NotBlank String resourceType,
        @NotBlank String resourceId
) {
}
