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
        name = "simulation_options",
        uniqueConstraints = @UniqueConstraint(name = "uk_simulation_option", columnNames = {"simulation_node_id", "option_id"})
)
public class SimulationOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_node_id", nullable = false)
    private SimulationNodeEntity simulationNode;

    @Column(name = "option_id", nullable = false, length = 120)
    private String optionId;

    @Column(name = "text", nullable = false, length = 800)
    private String text;

    @Column(name = "next_node_id", length = 120)
    private String nextNodeId;

    @Column(name = "critical", nullable = false)
    private boolean critical;

    @Column(name = "audit_note", nullable = false, length = 1000)
    private String auditNote;

    @OneToMany(mappedBy = "simulationOption", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<OptionCompetencyScoreEntity> competencyScores = new LinkedHashSet<>();
}
