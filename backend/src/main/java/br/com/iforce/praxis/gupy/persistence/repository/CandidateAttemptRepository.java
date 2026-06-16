package br.com.iforce.praxis.gupy.persistence.repository;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CandidateAttemptRepository extends JpaRepository<CandidateAttemptEntity, String> {

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByIdempotencyKey(String idempotencyKey);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByResultId(String resultId);

    @Override
    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findById(String id);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByTenantIdAndResultId(String tenantId, String resultId);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByTenantIdAndId(String tenantId, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"answers", "resultItems"})
    @Query("select c from CandidateAttemptEntity c where c.id = :id")
    Optional<CandidateAttemptEntity> findByIdForUpdate(@Param("id") String id);

    long countBySimulationVersionId(Long simulationVersionId);

    long countBySimulationVersionIdAndStatus(Long simulationVersionId, AttemptStatus status);

    long countByTenantIdAndSimulationVersionId(String tenantId, Long simulationVersionId);

    long countByTenantIdAndSimulationVersionIdAndStatus(
            String tenantId,
            Long simulationVersionId,
            AttemptStatus status
    );
}
