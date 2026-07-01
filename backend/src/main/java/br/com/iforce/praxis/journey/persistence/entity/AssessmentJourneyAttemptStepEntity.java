package br.com.iforce.praxis.journey.persistence.entity;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStepStatus;

import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;

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


/**
 * Relaciona a tentativa da jornada com a tentativa individual de cada teste.
 *
 * <p>Cada etapa da jornada aponta para a {@code CandidateAttempt} gerada para o
 * teste correspondente, preservando intacto o histórico individual de cada
 * teste.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assessment_journey_attempt_steps")
public class AssessmentJourneyAttemptStepEntity implements EmpresaAwareEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journey_attempt_id", nullable = false)
    private AssessmentJourneyAttemptEntity journeyAttempt;

    @Column(name = "journey_step_id", nullable = false)
    private Long journeyStepId;

    @Column(name = "simulation_id", nullable = false, length = 120)
    private String simulationId;

    @Column(name = "simulation_version_number", nullable = false)
    private int simulationVersionNumber;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "candidate_attempt_id", length = 80)
    private String candidateAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AssessmentJourneyStepStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
