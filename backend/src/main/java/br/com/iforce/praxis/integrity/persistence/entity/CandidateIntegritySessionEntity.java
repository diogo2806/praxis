package br.com.iforce.praxis.integrity.persistence.entity;

import br.com.iforce.praxis.integrity.model.IntegrityInputMode;
import br.com.iforce.praxis.integrity.model.IntegritySessionStatus;
import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;
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
@Table(name = "candidate_integrity_sessions")
public class CandidateIntegritySessionEntity implements EmpresaAwareEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "candidate_attempt_id", nullable = false, length = 80)
    private String candidateAttemptId;

    @Column(name = "client_session_id", nullable = false, length = 80)
    private String clientSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IntegritySessionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "last_heartbeat_at", nullable = false)
    private Instant lastHeartbeatAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;

    @Column(name = "user_agent_category", nullable = false, length = 40)
    private String userAgentCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_mode", nullable = false, length = 20)
    private IntegrityInputMode inputMode;
}
