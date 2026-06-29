package br.com.iforce.praxis.billing.persistence.entity;

import br.com.iforce.praxis.billing.model.BillingEventType;
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
 * Evento financeiro append-only. Guarda o que foi confirmado junto ao Mercado Pago (ou ajustes
 * manuais), incluindo o payload bruto consultado. Nunca é editado nem excluído.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenant_billing_events")
public class TenantBillingEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private BillingEventType eventType;

    /** Tipo do recurso no Mercado Pago: payment, preapproval, etc. */
    @Column(name = "mp_resource_type", length = 40)
    private String mpResourceType;

    @Column(name = "mp_resource_id", length = 120)
    private String mpResourceId;

    @Column(name = "external_reference", length = 200)
    private String externalReference;

    /** Status reportado pelo Mercado Pago (approved, rejected, pending, refunded, ...). */
    @Column(name = "mp_status", length = 60)
    private String mpStatus;

    @Column(name = "amount_cents")
    private Long amountCents;

    @Column(name = "currency", length = 8)
    private String currency;

    /** x-request-id capturado do webhook, quando originado de notificação. */
    @Column(name = "request_id", length = 120)
    private String requestId;

    @Column(name = "raw_payload", length = 8000)
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
