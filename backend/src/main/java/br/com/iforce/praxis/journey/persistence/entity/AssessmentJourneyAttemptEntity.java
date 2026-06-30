package br.com.iforce.praxis.journey.persistence.entity;

import br.com.iforce.praxis.journey.model.AssessmentJourneyAttemptStatus;

import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;

import jakarta.persistence.CascadeType;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.EnumType;

import jakarta.persistence.Enumerated;

import jakarta.persistence.Id;

import jakarta.persistence.OneToMany;

import jakarta.persistence.Table;

import lombok.Getter;

import lombok.NoArgsConstructor;

import lombok.Setter;


import java.time.Instant;

import java.util.LinkedHashSet;

import java.util.Set;


/**
 * Tentativa de um candidato em uma Jornada de Avaliação.
 *
 * <p>Controla o progresso geral da jornada. A jornada é concluída quando todos
 * os testes obrigatórios da sequência escolhida forem concluídos. Cada teste
 * interno continua gerando a sua própria {@code CandidateAttempt}, vinculada
 * por {@link AssessmentJourneyAttemptStepEntity}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assessment_journey_attempts")
public class AssessmentJourneyAttemptEntity implements EmpresaAwareEntity {

    @Id
    @Column(name = "id", nullable = false, length = 80)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "journey_id", nullable = false, length = 120)
    private String journeyId;

    @Column(name = "candidate_name", nullable = false, length = 160)
    private String candidateName;

    @Column(name = "candidate_email", nullable = false, length = 180)
    private String candidateEmail;

    @Column(name = "sequence_key", nullable = false, length = 80)
    private String sequenceKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AssessmentJourneyAttemptStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @OneToMany(mappedBy = "journeyAttempt", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AssessmentJourneyAttemptStepEntity> steps = new LinkedHashSet<>();
}
