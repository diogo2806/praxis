package br.com.iforce.praxis.simulation.persistence.entity;

import br.com.iforce.praxis.gupy.model.ResultTier;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "simulation_competencies",
        uniqueConstraints = @UniqueConstraint(name = "uk_simulation_competency", columnNames = {"simulation_version_id", "name"})
)
public class SimulationCompetencyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_version_id", nullable = false)
    private SimulationVersionEntity simulationVersion;

    @Column(name = "name", nullable = false, length = 140)
    private String name;

    @Column(name = "weight", nullable = false)
    private double weight;

    @Column(name = "target_score", nullable = false)
    private int targetScore = 70;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", nullable = false, length = 20)
    private ResultTier tier = ResultTier.MAJOR;
}
