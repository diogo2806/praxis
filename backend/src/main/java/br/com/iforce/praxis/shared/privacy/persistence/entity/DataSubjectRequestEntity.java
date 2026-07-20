package br.com.iforce.praxis.shared.privacy.persistence.entity;

import br.com.iforce.praxis.candidate.dto.DataSubjectRequestType;
import br.com.iforce.praxis.shared.privacy.model.ComplianceRequestStatus;
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
@Table(name = "data_subject_requests")
public class DataSubjectRequestEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "attempt_id", nullable = false, length = 80)
    private String attemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 60)
    private DataSubjectRequestType requestType;

    @Column(name = "contact", length = 320)
    private String contact;

    @Column(name = "details", length = 1000)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ComplianceRequestStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "assigned_user_id", length = 120)
    private String assignedUserId;

    @Column(name = "resolution", length = 2000)
    private String resolution;

    @Column(name = "denial_reason", length = 2000)
    private String denialReason;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
