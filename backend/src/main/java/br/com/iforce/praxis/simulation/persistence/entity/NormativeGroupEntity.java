package br.com.iforce.praxis.simulation.persistence.entity;

import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;
import br.com.iforce.praxis.simulation.model.NormativeGroupStatus;
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
@Table(name = "talent_normative_groups")
public class NormativeGroupEntity implements EmpresaAwareEntity {

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

    @Column(name = "name", nullable = false, length = 160)
    private String name;

    @Column(name = "job_title", nullable = false, length = 160)
    private String jobTitle;

    @Column(name = "seniority", length = 100)
    private String seniority;

    @Column(name = "gupy_job_id")
    private Long gupyJobId;

    @Column(name = "population_description", nullable = false, length = 1000)
    private String populationDescription;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "minimum_sample", nullable = false)
    private int minimumSample = 30;

    @Column(name = "path_compatibility_confirmed", nullable = false)
    private boolean pathCompatibilityConfirmed;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private NormativeGroupStatus status = NormativeGroupStatus.DRAFT;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "approved_by", length = 120)
    private String approvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
