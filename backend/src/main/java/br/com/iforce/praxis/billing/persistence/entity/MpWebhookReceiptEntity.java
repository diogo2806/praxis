package br.com.iforce.praxis.billing.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Recibo de notificação do Mercado Pago, usado para idempotência do webhook. A chave
 * {@code notificationId} é única: notificações repetidas não reprocessam o efeito financeiro.
 * Guarda o payload bruto recebido.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "mp_webhook_receipts")
public class MpWebhookReceiptEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /** Chave de deduplicação: combina tópico e id do recurso da notificação. */
    @Column(name = "notification_id", nullable = false, unique = true, length = 200)
    private String notificationId;

    @Column(name = "topic", length = 60)
    private String topic;

    @Column(name = "resource_id", length = 120)
    private String resourceId;

    @Column(name = "request_id", length = 120)
    private String requestId;

    @Column(name = "raw_payload", length = 8000)
    private String rawPayload;

    @Column(name = "processed", nullable = false)
    private boolean processed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
