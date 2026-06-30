package br.com.iforce.praxis.shared.notification.persistence.entity;

import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;

import br.com.iforce.praxis.shared.notification.model.InAppNotificationType;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.EnumType;

import jakarta.persistence.Enumerated;

import jakarta.persistence.GeneratedValue;

import jakarta.persistence.GenerationType;

import jakarta.persistence.Id;

import jakarta.persistence.Table;

import jakarta.persistence.UniqueConstraint;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;


import java.time.Instant;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "in_app_notifications",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_dlq_recipient",
                columnNames = {"empresa_id", "outbox_event_id", "recipient_user_id", "type"}
        )
)
public class InAppNotificationEntity implements EmpresaAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 80)
    private InAppNotificationType type;

    @Column(name = "title", nullable = false, length = 180)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "candidate_attempt_id", nullable = false, length = 80)
    private String candidateAttemptId;

    @Column(name = "candidate_name", nullable = false, length = 160)
    private String candidateName;

    @Column(name = "candidate_email", nullable = false, length = 180)
    private String candidateEmail;

    @Column(name = "outbox_event_id", nullable = false)
    private Long outboxEventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;
}
