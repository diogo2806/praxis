package br.com.iforce.praxis.simulation.persistence.entity;

import br.com.iforce.praxis.shared.model.MediaType;

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

    @Column(name = "timeout_next_node_id", length = 120)
    private String timeoutNextNodeId;

    @Column(name = "position_x")
    private Double positionX;

    @Column(name = "position_y")
    private Double positionY;

    @Column(name = "is_final", nullable = false)
    private boolean isFinal;

    @Column(name = "report_text", length = 2000)
    private String reportText;

    @Column(name = "plain_text_description", length = 1500)
    private String plainTextDescription;

    @Column(name = "audio_description_url", length = 1000)
    private String audioDescriptionUrl;

    @Column(name = "media_url", length = 1000)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", length = 16)
    private MediaType mediaType;

    @OneToMany(mappedBy = "simulationNode", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SimulationOptionEntity> options = new LinkedHashSet<>();

    public void setTimeLimitSeconds(Integer timeLimitSeconds) {
        this.timeLimitSeconds = timeLimitSeconds != null && timeLimitSeconds == 0
                ? null
                : timeLimitSeconds;
    }
}
