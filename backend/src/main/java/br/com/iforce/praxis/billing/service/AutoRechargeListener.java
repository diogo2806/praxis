package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.billing.event.CreditConsumedEvent;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Async;

import org.springframework.stereotype.Component;

import org.springframework.transaction.event.TransactionPhase;

import org.springframework.transaction.event.TransactionalEventListener;


/**
 * Ponte entre o consumo de créditos e a recarga automática.
 *
 * <p>Na visão do processo, é o "sensor de saldo baixo": quando a {@link CreditService} anuncia que
 * um cliente pré-pago consumiu um crédito, este componente aciona a avaliação de recarga
 * automática. Ele age de propósito <b>depois</b> que a transação do consumo é confirmada
 * (AFTER_COMMIT), para nunca recarregar em cima de um débito que acabou revertido, e de forma
 * <b>assíncrona</b>, para não fazer o candidato esperar a resposta do Mercado Pago ao concluir a
 * avaliação.</p>
 *
 * <p>Manter esta ponte separada evita um ciclo de dependências: a {@link CreditService} apenas
 * publica um aviso e não conhece o Mercado Pago, enquanto a decisão de cobrar vive na
 * {@link AutoRechargeService}.</p>
 */
@Component
public class AutoRechargeListener {

    private static final Logger log = LoggerFactory.getLogger(AutoRechargeListener.class);

    private final AutoRechargeService autoRechargeService;

    public AutoRechargeListener(AutoRechargeService autoRechargeService) {
        this.autoRechargeService = autoRechargeService;
    }

    /**
     * Reage ao aviso de crédito consumido avaliando uma possível recarga automática. Roda após o
     * commit do consumo e fora da linha de resposta do candidato; qualquer falha é registrada e
     * contida aqui para não afetar o fluxo da avaliação.
     *
     * @param event aviso com o cliente e o saldo restante após o consumo
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreditConsumed(CreditConsumedEvent event) {
        try {
            autoRechargeService.maybeRecharge(event.empresaId());
        } catch (RuntimeException failure) {
            log.error("Falha ao avaliar recarga automática da empresa {}.", event.empresaId(), failure);
        }
    }
}
