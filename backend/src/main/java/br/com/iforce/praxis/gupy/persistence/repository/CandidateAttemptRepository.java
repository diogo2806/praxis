package br.com.iforce.praxis.gupy.persistence.repository;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.EntityGraph;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.repository.query.Param;


import java.time.Instant;

import java.util.List;

import java.util.Optional;


public interface CandidateAttemptRepository extends JpaRepository<CandidateAttemptEntity, String>, JpaSpecificationExecutor<CandidateAttemptEntity> {

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByEmpresaIdAndIdempotencyKey(String empresaId, String idempotencyKey);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByEmpresaIdAndResultId(String empresaId, String resultId);

    @EntityGraph(attributePaths = {"answers", "resultItems"})
    Optional<CandidateAttemptEntity> findByEmpresaIdAndId(String empresaId, String id);

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

    // --- Contagem de uso para o painel administrativo (avaliações concluídas) ---

    /** Total de tentativas concluídas de um empresa dentro do período informado. */
    long countByEmpresaIdAndStatusAndFinishedAtBetween(
            String empresaId,
            AttemptStatus status,
            Instant from,
            Instant to
    );

    /** Total de tentativas concluídas de um empresa desde o instante informado. */
    long countByEmpresaIdAndStatusAndFinishedAtAfter(String empresaId, AttemptStatus status, Instant after);

    /** Total de tentativas concluídas de um empresa em toda a sua história. */
    long countByEmpresaIdAndStatus(String empresaId, AttemptStatus status);

    long countByEmpresaIdAndStatusIn(String empresaId, List<AttemptStatus> statuses);

    /** Total de tentativas concluídas na plataforma inteira dentro do período informado. */
    long countByStatusAndFinishedAtBetween(AttemptStatus status, Instant from, Instant to);

    /** Última conclusão de tentativa de um empresa, para a aba de uso. */
    @Query("SELECT MAX(c.finishedAt) FROM CandidateAttemptEntity c WHERE c.empresaId = :empresaId AND c.status = :status")
    Optional<Instant> findLastFinishedAt(
            @Param("empresaId") String empresaId,
            @Param("status") AttemptStatus status
    );

    /**
     * Ranking de uso por empresa no período: cada linha é {@code [empresaId, total]} ordenado
     * pela maior quantidade de tentativas concluídas. Usado no dashboard administrativo.
     */
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

    long countByEmpresaIdAndSimulationVersionId(String empresaId, Long simulationVersionId);

    /**
     * Tentativas de uma versão num determinado estado, já com respostas e itens de
     * resultado carregados. Usado pela calibração estatística, que precisa percorrer
     * as respostas escolhidas e as notas por competência de cada tentativa concluída.
     */
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
