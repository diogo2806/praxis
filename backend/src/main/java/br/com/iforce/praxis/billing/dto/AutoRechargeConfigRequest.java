package br.com.iforce.praxis.billing.dto;

import jakarta.validation.constraints.Positive;


/**
 * Pedido do cliente para ligar/desligar e ajustar a recarga automática (auto-top-up) do plano
 * pré-pago AVULSO.
 *
 * <p>Ao ligar ({@code enabled = true}), o pacote de créditos e o cartão salvo passam a ser
 * obrigatórios — a regra completa é validada no serviço. O cartão nunca trafega em claro: são
 * enviadas apenas as referências opacas do Mercado Pago ({@code mpCustomerId}/{@code mpCardId}),
 * obtidas num cadastro prévio de meio de pagamento.</p>
 *
 * @param enabled se a recarga automática deve ficar ligada
 * @param thresholdCredits nível crítico de saldo que dispara a recarga (avaliações)
 * @param planId pacote de créditos AVULSO a comprar na recarga
 * @param mpCustomerId referência do cliente/pagador salvo no Mercado Pago
 * @param mpCardId referência do cartão salvo no Mercado Pago
 */
public record AutoRechargeConfigRequest(
        boolean enabled,
        @Positive(message = "O nível crítico de saldo deve ser positivo.") int thresholdCredits,
        Long planId,
        String mpCustomerId,
        String mpCardId
) {
}
