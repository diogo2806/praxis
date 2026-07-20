package br.com.iforce.praxis.shared.privacy.persistence.entity;

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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "privacy_incidents")
public class PrivacyIncidentEntity {

    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum Status {
        OPEN,
        INVESTIGATING,
        CONTAINED,
        NOTIFICATION_REQUIRED,
        CLOSED
    }

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "description", nullable = false, length = 4000)
    private String description;

    @Column(name = "affected_data", length = 4000)
    private String affectedData;

    @Column(name = "affected_subjects_estimate")
    private Integer affectedSubjectsEstimate;

    @Column(name = "risk_assessment", length = 4000)
    private String riskAssessment;

    @Column(name = "owner_user_id", length = 120)
    private String ownerUserId;

    @Column(name = "controller_notified_at")
    private Instant controllerNotifiedAt;

    @Column(name = "anpd_notified_at")
    private Instant anpdNotifiedAt;

    @Column(name = "subjects_notified_at")
    private Instant subjectsNotifiedAt;

    @Column(name = "retention_until", nullable = false)
    private Instant retentionUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
