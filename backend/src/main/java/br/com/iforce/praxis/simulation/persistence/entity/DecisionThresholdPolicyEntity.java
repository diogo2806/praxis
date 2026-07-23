package br.com.iforce.praxis.simulation.persistence.entity;

import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;
import br.com.iforce.praxis.simulation.model.CutScorePolicyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "talent_cut_score_policies")
public class DecisionThresholdPolicyEntity implements EmpresaAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "simulation_id", nullable = false, length = 120)
    private String simulationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_version_id", nullable = false)
    private SimulationVersionEntity simulationVersion;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "population_description", nullable = false, length = 1000)
    private String populationDescription;

    @Column(name = "justification", nullable = false, length = 2000)
    private String justification;

    @Column(name = "evidence", nullable = false, length = 2000)
    private String evidence;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CutScorePolicyStatus status = CutScorePolicyStatus.DRAFT;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
