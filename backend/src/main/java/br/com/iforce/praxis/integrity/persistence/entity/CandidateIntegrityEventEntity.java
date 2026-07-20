package br.com.iforce.praxis.integrity.persistence.entity;

import br.com.iforce.praxis.integrity.model.IntegrityEventType;
import br.com.iforce.praxis.integrity.model.IntegrityInputMode;
import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;
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

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidate_integrity_events")
public class CandidateIntegrityEventEntity implements EmpresaAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "candidate_attempt_id", nullable = false, length = 80)
    private String candidateAttemptId;

    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private IntegrityEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_mode", length = 20)
    private IntegrityInputMode inputMode;

    @Column(name = "visibility_state", length = 20)
    private String visibilityState;

    @Column(name = "sequence_number")
    private Long sequenceNumber;

    @Column(name = "detail", length = 120)
    private String detail;
}
