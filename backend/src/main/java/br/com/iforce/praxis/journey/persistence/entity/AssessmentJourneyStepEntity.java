package br.com.iforce.praxis.journey.persistence.entity;

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


/**
 * Cada teste (simulação publicada) dentro de uma Jornada de Avaliação.
 *
 * <p>Aponta sempre para uma versão publicada da simulação, preservando o
 * número da versão usado. A {@code sequenceKey} identifica o caminho da jornada
 * (ex.: {@code A}, {@code B}, {@code principal}) e o {@code orderIndex} define a
 * ordem do teste dentro daquela sequência.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assessment_journey_steps")
public class AssessmentJourneyStepEntity implements EmpresaAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journey_id", nullable = false)
    private AssessmentJourneyEntity journey;

    @Column(name = "simulation_id", nullable = false, length = 120)
    private String simulationId;

    @Column(name = "simulation_version_number", nullable = false)
    private int simulationVersionNumber;

    @Column(name = "sequence_key", nullable = false, length = 80)
    private String sequenceKey;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
