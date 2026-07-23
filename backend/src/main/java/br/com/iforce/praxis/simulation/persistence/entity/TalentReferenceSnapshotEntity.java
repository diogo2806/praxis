package br.com.iforce.praxis.simulation.persistence.entity;

import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "talent_reference_snapshots")
public class TalentReferenceSnapshotEntity implements EmpresaAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "attempt_id", nullable = false, length = 80)
    private String attemptId;

    @Column(name = "simulation_id", nullable = false, length = 120)
    private String simulationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_version_id", nullable = false)
    private SimulationVersionEntity simulationVersion;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "target_profile_json", nullable = false, columnDefinition = "TEXT")
    private String targetProfileJson;

    @Column(name = "normative_reference_json", columnDefinition = "TEXT")
    private String normativeReferenceJson;

    @Column(name = "cut_score_policy_json", columnDefinition = "TEXT")
    private String decisionThresholdJson;

    @Column(name = "captured_at", nullable = false)
    private Instant capturedAt;
}
