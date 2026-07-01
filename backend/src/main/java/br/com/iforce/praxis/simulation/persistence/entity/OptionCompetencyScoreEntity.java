package br.com.iforce.praxis.simulation.persistence.entity;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

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
        name = "option_competency_scores",
        uniqueConstraints = @UniqueConstraint(name = "uk_option_competency_score", columnNames = {"simulation_option_id", "competency_name"})
)
public class OptionCompetencyScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_option_id", nullable = false)
    private SimulationOptionEntity simulationOption;

    @Column(name = "competency_name", nullable = false, length = 140)
    private String competencyName;

    @Column(name = "score", nullable = false)
    private int score;
}
