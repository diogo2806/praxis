package br.com.iforce.praxis.gupy.persistence.repository;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidateAttemptRepository extends JpaRepository<CandidateAttemptEntity, String> {

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByResultId(String resultId);

    @Override
    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findById(String id);

    long countBySimulationVersionId(Long simulationVersionId);

    long countBySimulationVersionIdAndStatus(Long simulationVersionId, AttemptStatus status);
}
