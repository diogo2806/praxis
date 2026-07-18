package br.com.iforce.praxis.gupy.persistence.repository;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CandidateAttemptRepository extends JpaRepository<CandidateAttemptEntity, String>, JpaSpecificationExecutor<CandidateAttemptEntity> {

    @Override
    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findById(String id);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByEmpresaIdAndIdempotencyKey(String empresaId, String idempotencyKey);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByEmpresaIdAndResultId(String empresaId, String resultId);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByEmpresaIdAndId(String empresaId, String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CandidateAttemptEntity> findOneByEmpresaIdAndId(String empresaId, String id);

    @EntityGraph(attributePaths = {"resultItems"})
    @Query("SELECT c FROM CandidateAttemptEntity c WHERE c.id IN :ids")
    List<CandidateAttemptEntity> findAllByIdInWithResultItems(@Param("ids") List<String> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"answers", "resultItems"})
    @Query("SELECT c FROM CandidateAttemptEntity c WHERE c.empresaId = :empresaId AND c.id = :id")
    Optional<CandidateAttemptEntity> findByEmpresaIdAndIdForUpdate(
            @Param("empresaId") String empresaId,
            @Param("id") String id
    );

    long countByEmpresaIdAndSimulationId(String empresaId, String simulationId);

    long countByEmpresaIdAndStatusAndFinishedAtBetween(
            String empresaId,
            AttemptStatus status,
            Instant from,
            Instant to
    );

    long countByEmpresaIdAndStatusAndFinishedAtAfter(String empresaId, AttemptStatus status, Instant after);

    long countByEmpresaIdAndStatus(String empresaId, AttemptStatus status);

    long countByEmpresaIdAndStatusIn(String empresaId, List<AttemptStatus> statuses);

    long countByEmpresaIdAndCallbackUrlIsNotNull(String empresaId);

    long countByEmpresaIdAndCallbackUrlIsNotNullAndStatus(String empresaId, AttemptStatus status);

    long countByEmpresaIdAndCallbackUrlIsNotNullAndResultWebhookUrlIsNotNull(String empresaId);

    @Query("SELECT MAX(c.createdAt) FROM CandidateAttemptEntity c WHERE c.empresaId = :empresaId AND c.callbackUrl IS NOT NULL")
    Optional<Instant> findLastGupyAttemptCreatedAt(@Param("empresaId") String empresaId);

    long countByStatusAndFinishedAtBetween(AttemptStatus status, Instant from, Instant to);

    @Query("SELECT MAX(c.finishedAt) FROM CandidateAttemptEntity c WHERE c.empresaId = :empresaId AND c.status = :status")
    Optional<Instant> findLastFinishedAt(
            @Param("empresaId") String empresaId,
            @Param("status") AttemptStatus status
    );

    @Query("""
            SELECT c.empresaId, COUNT(c)
            FROM CandidateAttemptEntity c
            WHERE c.status = :status
              AND c.finishedAt BETWEEN :from AND :to
            GROUP BY c.empresaId
            ORDER BY COUNT(c) DESC
            """)
    List<Object[]> findTopUsageEmpresas(
            @Param("status") AttemptStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );

    /**
     * Agrega o uso de vários clientes em uma única consulta para a listagem administrativa.
     */
    @Query("""
            SELECT c.empresaId, COUNT(c)
            FROM CandidateAttemptEntity c
            WHERE c.empresaId IN :empresaIds
              AND c.status = :status
              AND c.finishedAt BETWEEN :from AND :to
            GROUP BY c.empresaId
            """)
    List<Object[]> countByEmpresaIdsAndStatusAndFinishedAtBetween(
            @Param("empresaIds") List<String> empresaIds,
            @Param("status") AttemptStatus status,
            @Param("from") Instant from,
            @Param("to") Instant to
    );

    /**
     * Calcula, em lote, o volume atual e anterior usado pelo indicador de saúde dos clientes.
     */
    @Query("""
            SELECT c.empresaId,
                   SUM(CASE WHEN c.finishedAt >= :currentStart AND c.finishedAt <= :currentEnd THEN 1 ELSE 0 END),
                   SUM(CASE WHEN c.finishedAt >= :previousStart AND c.finishedAt < :currentStart THEN 1 ELSE 0 END)
            FROM CandidateAttemptEntity c
            WHERE c.empresaId IN :empresaIds
              AND c.status = :status
              AND c.finishedAt >= :previousStart
              AND c.finishedAt <= :currentEnd
            GROUP BY c.empresaId
            """)
    List<Object[]> summarizeHealthPeriods(
            @Param("empresaIds") List<String> empresaIds,
            @Param("status") AttemptStatus status,
            @Param("previousStart") Instant previousStart,
            @Param("currentStart") Instant currentStart,
            @Param("currentEnd") Instant currentEnd
    );

    /**
     * Busca, em lote, a última conclusão registrada para cada cliente.
     */
    @Query("""
            SELECT c.empresaId, MAX(c.finishedAt)
            FROM CandidateAttemptEntity c
            WHERE c.empresaId IN :empresaIds
              AND c.status = :status
            GROUP BY c.empresaId
            """)
    List<Object[]> findLastFinishedAtByEmpresaIds(
            @Param("empresaIds") List<String> empresaIds,
            @Param("status") AttemptStatus status
    );

    long countByEmpresaIdAndSimulationVersionId(String empresaId, Long simulationVersionId);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    List<CandidateAttemptEntity> findByEmpresaIdAndSimulationVersionIdAndStatus(
            String empresaId,
            Long simulationVersionId,
            AttemptStatus status
    );

    long countByEmpresaIdAndSimulationVersionIdAndStatus(
            String empresaId,
            Long simulationVersionId,
            AttemptStatus status
    );

    long countByEmpresaIdAndSimulationIdAndStatus(
            String empresaId,
            String simulationId,
            AttemptStatus status
    );

    List<CandidateAttemptEntity> findByEmpresaIdOrderByCreatedAtDesc(String empresaId, Pageable pageable);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    List<CandidateAttemptEntity> findByEmpresaIdAndStatusInOrderByCreatedAtDesc(
            String empresaId,
            List<AttemptStatus> statuses,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM CandidateAttemptEntity c
            WHERE c.empresaId = :empresaId
              AND c.status IN :statuses
              AND c.finishedAt IS NOT NULL
              AND c.finishedAt < :finishedBefore
              AND c.anonymizedAt IS NULL
            ORDER BY c.finishedAt ASC
            """)
    List<CandidateAttemptEntity> findRetentionCandidatesForEmpresa(
            @Param("empresaId") String empresaId,
            @Param("statuses") List<AttemptStatus> statuses,
            @Param("finishedBefore") Instant finishedBefore,
            Pageable pageable
    );
}
