package br.com.iforce.praxis.simulation.persistence.entity;

import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;

import jakarta.persistence.CascadeType;

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

import jakarta.persistence.OneToMany;

import jakarta.persistence.Table;

import jakarta.persistence.UniqueConstraint;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;


import java.time.Instant;

import java.util.LinkedHashSet;

import java.util.Set;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "simulation_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_simulation_version_number", columnNames = {"simulation_id", "version_number"})
)
public class SimulationVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_id", nullable = false)
    private SimulationEntity simulation;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private SimulationVersionStatus status;

    @Column(name = "root_node_id", nullable = false, length = 120)
    private String rootNodeId;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "simulationVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SimulationCompetencyEntity> competencies = new LinkedHashSet<>();

    @OneToMany(mappedBy = "simulationVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SimulationNodeEntity> nodes = new LinkedHashSet<>();
}
