package br.com.iforce.praxis.simulation.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "simulation_nodes",
        uniqueConstraints = @UniqueConstraint(name = "uk_simulation_node", columnNames = {"simulation_version_id", "node_id"})
)
public class SimulationNodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_version_id", nullable = false)
    private SimulationVersionEntity simulationVersion;

    @Column(name = "node_id", nullable = false, length = 120)
    private String nodeId;

    @Column(name = "turn_index", nullable = false)
    private int turnIndex;

    @Column(name = "speaker", nullable = false, length = 120)
    private String speaker;

    @Column(name = "message", nullable = false, length = 1200)
    private String message;

    @Column(name = "time_limit_seconds")
    private Integer timeLimitSeconds;

    @OneToMany(mappedBy = "simulationNode", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SimulationOptionEntity> options = new LinkedHashSet<>();
}
