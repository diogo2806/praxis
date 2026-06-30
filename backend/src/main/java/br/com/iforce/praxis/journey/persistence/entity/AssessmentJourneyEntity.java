package br.com.iforce.praxis.journey.persistence.entity;

import br.com.iforce.praxis.journey.model.AssessmentJourneyStatus;
import br.com.iforce.praxis.shared.jpa.TenantAwareEntity;
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
 * Jornada de Avaliação: agrupa várias simulações publicadas em uma experiência
 * maior, sem substituir a entidade de simulação/teste individual.
 *
 * <p>É uma camada de orquestração acima de {@code Simulation} /
 * {@code SimulationVersion}: a jornada apenas organiza testes já publicados em
 * uma ou mais sequências de execução.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assessment_journeys")
public class AssessmentJourneyEntity implements TenantAwareEntity {

    @Id
    @Column(name = "id", nullable = false, length = 120)
    private String id;

    @Column(name = "tenant_id", nullable = false, length = 120)
    private String tenantId;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private AssessmentJourneyStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "journey", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AssessmentJourneyStepEntity> steps = new LinkedHashSet<>();
}
