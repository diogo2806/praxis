package br.com.iforce.praxis.billing.model;

/** Motivo de um lançamento no ledger de créditos (append-only). */
public enum CreditLedgerReason {
    /** Crédito adicionado após compra confirmada no Mercado Pago. */
    PURCHASE,
    /** Crédito consumido por uma avaliação concluída (1 crédito por tentativa). */
    CONSUMPTION,
    /** Ajuste manual operacional (raro; sempre auditado). */
    ADJUSTMENT
}
