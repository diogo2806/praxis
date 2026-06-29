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

import java.time.Instant;
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

    long countByTenantIdAndSimulationId(String tenantId, String simulationId);

    // --- Contagem de uso para o painel administrativo (avaliações concluídas) ---

    /** Total de tentativas concluídas de um tenant dentro do período informado. */
    long countByTenantIdAndStatusAndFinishedAtBetween(
            String tenantId,
            AttemptStatus status,
            Instant from,
            Instant to
    );

    /** Total de tentativas concluídas de um tenant desde o instante informado. */
    long countByTenantIdAndStatusAndFinishedAtAfter(String tenantId, AttemptStatus status, Instant after);

    /** Total de tentativas concluídas de um tenant em toda a sua história. */
    long countByTenantIdAndStatus(String tenantId, AttemptStatus status);

    /** Total de tentativas concluídas na plataforma inteira dentro do período informado. */
    long countByStatusAndFinishedAtBetween(AttemptStatus status, Instant from, Instant to);

    /** Última conclusão de tentativa de um tenant, para a aba de uso. */
    @Query("SELECT MAX(c.finishedAt) FROM CandidateAttemptEntity c WHERE c.tenantId = :tenantId AND c.status = :status")
    Optional<Instant> findLastFinishedAt(
            @Param("tenantId") String tenantId,
            @Param("status") AttemptStatus status
    );

    /**
     * Ranking de uso por tenant no período: cada linha é {@code [tenantId, total]} ordenado
     * pela maior quantidade de tentativas concluídas. Usado no dashboard administrativo.
     */
    @Query("""
            SELECT c.tenantId, COUNT(c)
            FROM CandidateAttemptEntity c
            WHERE c.status = :status
              AND c.finishedAt BETWEEN :from AND :to
            GROUP BY c.tenantId
            ORDER BY COUNT(c) DESC
            """)
    List<Object[]> findTopUsageTenants(
            @Param("status") AttemptStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM CandidateAttemptEntity c
            WHERE c.tenantId = :tenantId
              AND c.status IN :statuses
              AND c.finishedAt IS NOT NULL
              AND c.finishedAt < :finishedBefore
              AND c.anonymizedAt IS NULL
            ORDER BY c.finishedAt ASC
            """)
    List<CandidateAttemptEntity> findRetentionCandidatesForTenant(
            @Param("tenantId") String tenantId,
            @Param("statuses") List<AttemptStatus> statuses,
            @Param("finishedBefore") Instant finishedBefore,
            Pageable pageable
    );

}
