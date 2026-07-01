package br.com.iforce.praxis.billing.persistence.entity;

import br.com.iforce.praxis.billing.model.CreditLedgerReason;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.EnumType;

import jakarta.persistence.Enumerated;

import jakarta.persistence.GeneratedValue;

import jakarta.persistence.GenerationType;

import jakarta.persistence.Id;

import jakarta.persistence.Table;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;


import java.time.Instant;


/**
 * Lançamento append-only do ledger de créditos. Toda alteração de saldo (compra ou consumo)
 * gera um lançamento; o consumo se relaciona com a tentativa concluída ({@code attemptId}).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "empresa_credit_ledger")
public class EmpresaCreditLedgerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    /** Variação do saldo: positivo na compra, negativo no consumo. */
    @Column(name = "delta", nullable = false)
    private int delta;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 40)
    private CreditLedgerReason reason;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    /** Tentativa concluída que originou o consumo (apenas consumo). */
    @Column(name = "attempt_id", length = 80)
    private String attemptId;

    /** Evento financeiro que originou a compra (apenas compra). */
    @Column(name = "billing_event_id")
    private Long billingEventId;

    @Column(name = "note", length = 300)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
