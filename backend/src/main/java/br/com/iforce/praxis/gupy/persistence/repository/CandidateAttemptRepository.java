package br.com.iforce.praxis.gupy.persistence.repository;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CandidateAttemptRepository extends JpaRepository<CandidateAttemptEntity, String> {

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByTenantIdAndResultId(String tenantId, String resultId);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByTenantIdAndId(String tenantId, String id);

    @EntityGraph(attributePaths = {"resultItems"})
    @Query("SELECT c FROM CandidateAttemptEntity c WHERE c.id IN :ids")
    List<CandidateAttemptEntity> findAllByIdInWithResultItems(@Param("ids") List<String> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"answers", "resultItems"})
    @Query("SELECT c FROM CandidateAttemptEntity c WHERE c.tenantId = :tenantId AND c.id = :id")
    Optional<CandidateAttemptEntity> findByTenantIdAndIdForUpdate(
            @Param("tenantId") String tenantId,
            @Param("id") String id
    );

    long countByTenantIdAndSimulationVersionId(String tenantId, Long simulationVersionId);

    long countByTenantIdAndSimulationVersionIdAndStatus(
            String tenantId,
            Long simulationVersionId,
            AttemptStatus status
    );

    List<CandidateAttemptEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    List<CandidateAttemptEntity> findByTenantIdAndStatusInOrderByCreatedAtDesc(
            String tenantId,
            List<AttemptStatus> statuses,
            Pageable pageable
    );

}
