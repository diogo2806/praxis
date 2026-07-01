package br.com.iforce.praxis.billing.persistence.entity;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;

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
 * Assinatura recorrente (plano PROFISSIONAL) vinculada a um preapproval do Mercado Pago.
 * O histórico de assinatura não é apagado: cancelamentos apenas mudam o status.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "empresa_subscriptions")
public class EmpresaSubscriptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "plan_id")
    private Long planId;

    @Column(name = "mp_preapproval_id", unique = true, length = 120)
    private String mpPreapprovalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private SubscriptionStatus status;

    /** Link de pagamento/autorização retornado pelo Mercado Pago, quando aplicável. */
    @Column(name = "init_point", length = 600)
    private String initPoint;

    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;

    @Column(name = "last_payment_at")
    private Instant lastPaymentAt;

    /** Fim da carência de inadimplência; após esse instante o cliente pode ser suspenso. */
    @Column(name = "grace_until")
    private Instant graceUntil;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
