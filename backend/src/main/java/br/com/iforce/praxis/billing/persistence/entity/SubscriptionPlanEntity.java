package br.com.iforce.praxis.billing.persistence.entity;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
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
 * Plano comercial real. AVULSO representa crédito pré-pago; PROFISSIONAL representa
 * assinatura recorrente. A periodicidade é mantida no próprio plano para que preço,
 * checkout e comunicação comercial usem a mesma regra.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 40)
    private CommercialPlanType planType;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "currency", nullable = false, length = 8)
    private String currency = "BRL";

    /** Quantidade de créditos concedidos por compra (somente AVULSO). */
    @Column(name = "credit_amount")
    private Integer creditAmount;

    /**
     * Intervalo de recorrência em meses. Para planos avulsos não há cobrança recorrente;
     * o valor padrão de 1 preserva compatibilidade com registros históricos.
     */
    @Column(name = "billing_interval_months", nullable = false)
    private int billingIntervalMonths = 1;

    /** Identificador opcional do plano de preapproval no Mercado Pago. */
    @Column(name = "mp_preapproval_plan_id", length = 120)
    private String mpPreapprovalPlanId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
