package br.com.iforce.praxis.billing.persistence.entity;

import br.com.iforce.praxis.billing.model.AutoRechargeStatus;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.EnumType;

import jakarta.persistence.Enumerated;

import jakarta.persistence.Id;

import jakarta.persistence.Table;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;


import java.time.Instant;


/**
 * Preferência de recarga automática (auto-top-up) de um cliente pré-pago (AVULSO).
 *
 * <p>Guarda como e quando recarregar sozinho: se está ligada, qual o nível crítico de saldo que
 * dispara a recarga, qual pacote de créditos comprar e quais as referências do cartão salvo no
 * Mercado Pago para a cobrança. O cartão em si nunca é guardado aqui — apenas os identificadores
 * opacos do Mercado Pago ({@code mpCustomerId}/{@code mpCardId}).</p>
 *
 * <p>Também mantém o estado do ciclo de cobrança ({@link AutoRechargeStatus}) e a marca da última
 * tentativa, que servem de trava contra cobrança dupla e de janela de espera (cooldown) entre
 * tentativas.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "empresa_auto_recharge_config")
public class EmpresaAutoRechargeConfigEntity {

    @Id
    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    /** Liga ou desliga a recarga automática para o cliente. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    /** Nível crítico de saldo: ao cair ABAIXO deste valor, dispara a recarga. */
    @Column(name = "threshold_credits", nullable = false)
    private int thresholdCredits = 5;

    /** Pacote de créditos AVULSO usado na recarga (referência a {@code subscription_plans}). */
    @Column(name = "plan_id")
    private Long planId;

    /** Identificador do cliente/pagador salvo no Mercado Pago (nunca o cartão em claro). */
    @Column(name = "mp_customer_id", length = 120)
    private String mpCustomerId;

    /** Identificador do cartão salvo no Mercado Pago (nunca o número do cartão). */
    @Column(name = "mp_card_id", length = 120)
    private String mpCardId;

    /** Estado do ciclo de recarga: ocioso ou com cobrança em andamento. */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AutoRechargeStatus status = AutoRechargeStatus.IDLE;

    /** {@code external_reference} da cobrança em andamento (idempotência com o Mercado Pago). */
    @Column(name = "pending_reference", length = 200)
    private String pendingReference;

    /** Momento da última tentativa de recarga (usado para cooldown e observabilidade). */
    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    /** Resumo legível do último resultado da recarga. */
    @Column(name = "last_outcome", length = 300)
    private String lastOutcome;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
