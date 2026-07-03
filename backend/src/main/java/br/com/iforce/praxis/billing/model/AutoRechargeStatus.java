package br.com.iforce.praxis.billing.model;

/**
 * Estado do ciclo de recarga automática de um cliente pré-pago (AVULSO).
 *
 * <p>Funciona como uma "trava de porta" para evitar cobrar o cartão do cliente duas vezes pelo
 * mesmo motivo: enquanto uma recarga está em andamento o estado fica {@link #PENDING} e nenhuma
 * nova cobrança é disparada; quando ela se resolve (aprovada, recusada ou falha), volta a
 * {@link #IDLE} e o cliente pode ser recarregado de novo se o saldo continuar baixo.</p>
 */
public enum AutoRechargeStatus {
    /** Ocioso: nenhuma cobrança em andamento; pronto para disparar uma recarga se preciso. */
    IDLE,
    /** Cobrança em andamento no Mercado Pago; aguardando confirmação (evita cobrança dupla). */
    PENDING
}
