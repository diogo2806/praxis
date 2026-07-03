package br.com.iforce.praxis.billing.dto;

import br.com.iforce.praxis.billing.model.AutoRechargeStatus;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaAutoRechargeConfigEntity;


import java.time.Instant;


/**
 * Visão da recarga automática que o cliente vê na sua tela de cobrança.
 *
 * <p>Mostra se está ligada, a partir de qual saldo age, qual pacote recarrega e se já há um cartão
 * salvo — sem nunca expor os identificadores do cartão. Também informa o estado do ciclo e o
 * resultado da última tentativa, para o cliente acompanhar.</p>
 *
 * @param enabled se a recarga automática está ligada
 * @param thresholdCredits nível crítico de saldo que dispara a recarga
 * @param planId pacote de créditos AVULSO usado na recarga (pode ser nulo se nunca configurado)
 * @param cardConfigured se há um cartão salvo pronto para ser cobrado
 * @param status estado atual do ciclo (ocioso ou cobrança em andamento)
 * @param lastTriggeredAt momento da última tentativa de recarga (nulo se nunca houve)
 * @param lastOutcome resumo legível do último resultado (nulo se nunca houve)
 */
public record AutoRechargeConfigResponse(
        boolean enabled,
        int thresholdCredits,
        Long planId,
        boolean cardConfigured,
        AutoRechargeStatus status,
        Instant lastTriggeredAt,
        String lastOutcome
) {

    /** Converte a configuração persistida na visão do cliente, ocultando os dados do cartão. */
    public static AutoRechargeConfigResponse from(EmpresaAutoRechargeConfigEntity config) {
        boolean cardConfigured = config.getMpCustomerId() != null && !config.getMpCustomerId().isBlank()
                && config.getMpCardId() != null && !config.getMpCardId().isBlank();
        return new AutoRechargeConfigResponse(
                config.isEnabled(),
                config.getThresholdCredits(),
                config.getPlanId(),
                cardConfigured,
                config.getStatus(),
                config.getLastTriggeredAt(),
                config.getLastOutcome());
    }
}
