package br.com.iforce.praxis.billing.persistence.entity;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.Id;

import jakarta.persistence.Table;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;


import java.time.Instant;


/**
 * Saldo atual de créditos de um cliente AVULSO. O saldo é sempre derivado de lançamentos no
 * ledger ({@code empresa_credit_ledger}) — nunca alterado sem um lançamento correspondente.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "empresa_credit_balances")
public class EmpresaCreditBalanceEntity {

    @Id
    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "balance", nullable = false)
    private int balance;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
