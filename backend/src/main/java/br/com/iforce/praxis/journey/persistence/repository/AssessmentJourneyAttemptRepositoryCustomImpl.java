package br.com.iforce.praxis.journey.persistence.repository;

import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptStepEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.Optional;

public class AssessmentJourneyAttemptRepositoryCustomImpl implements AssessmentJourneyAttemptRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<JourneyRedirectTarget> findJourneyRedirectTarget(
            String empresaId,
            String candidateAttemptId
    ) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<JourneyRedirectTarget> query = criteriaBuilder.createQuery(JourneyRedirectTarget.class);
        Root<AssessmentJourneyAttemptStepEntity> step = query.from(AssessmentJourneyAttemptStepEntity.class);

        query.select(criteriaBuilder.construct(
                JourneyRedirectTarget.class,
                step.get("journeyAttempt").get("id"),
                step.get("id")
        ));
        query.where(
                criteriaBuilder.equal(step.get("empresaId"), empresaId),
                criteriaBuilder.equal(step.get("journeyAttempt").get("empresaId"), empresaId),
                criteriaBuilder.equal(step.get("candidateAttemptId"), candidateAttemptId)
        );
        query.orderBy(
                criteriaBuilder.desc(step.get("journeyAttempt").get("createdAt")),
                criteriaBuilder.desc(step.get("id"))
        );

        return entityManager.createQuery(query)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }
}
