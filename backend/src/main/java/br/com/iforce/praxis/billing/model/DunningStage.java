package br.com.iforce.praxis.billing.model;

/**
 * Etapa da régua de cobrança inteligente (dunning) que originou uma notificação educativa de retry.
 *
 * <ul>
 *     <li>{@link #PAYMENT_FAILED}: a mensagem nasce direto do webhook do Mercado Pago, no momento em
 *         que um pagamento é recusado e o cliente entra em inadimplência (início da carência).</li>
 *     <li>{@link #RETRY_REMINDER}: lembrete recorrente enviado enquanto o cliente segue pendente de
 *         pagamento ou inadimplente, cobrindo os gaps antes da suspensão dura do acesso.</li>
 * </ul>
 */
public enum DunningStage {
    PAYMENT_FAILED,
    RETRY_REMINDER
}
