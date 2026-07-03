package br.com.iforce.praxis.shared.outbox.persistence.repository;

import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;


import java.time.Instant;

import java.util.List;

import java.util.Optional;


@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    /**
     * Reivindica um lote de eventos prontos, travando as linhas para que pollers
     * concorrentes (outras instâncias) as ignorem ({@code SKIP LOCKED}). Deve ser
     * chamada dentro de uma transação curta que muda o status para PROCESSING e libera
     * a trava no commit.
     *
     * <p>Além dos eventos PENDING/RETRYING já maduros (next_attempt_at &lt;= now), também
     * reivindica eventos PROCESSING órfãos — linhas deixadas travadas por uma instância
     * que morreu no meio do envio — cujo last_attempt_at é mais antigo que stuckBefore.</p>
     *
     * <p>Nomes de coluna conforme V17_1__create_outbox_events_table / V20.</p>
     */
    @Query(value = """
        SELECT * FROM outbox_events
        WHERE (status IN (:statuses) AND next_attempt_at <= :now)
           OR (status = 'PROCESSING' AND last_attempt_at <= :stuckBefore)
        ORDER BY created_at ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEventEntity> claimReadyBatch(
        @Param("statuses") List<String> statuses,
        @Param("now") Instant now,
        @Param("stuckBefore") Instant stuckBefore,
        @Param("batchSize") int batchSize
    );

    /**
     * Variante por empresa de {@link #claimReadyBatch}, usada no disparo manual de
     * reprocessamento por um administrador do empresa.
     */
    @Query(value = """
        SELECT * FROM outbox_events
        WHERE empresa_id = :empresaId
          AND ((status IN (:statuses) AND next_attempt_at <= :now)
            OR (status = 'PROCESSING' AND last_attempt_at <= :stuckBefore))
        ORDER BY created_at ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<OutboxEventEntity> claimReadyBatchForEmpresa(
        @Param("empresaId") String empresaId,
        @Param("statuses") List<String> statuses,
        @Param("now") Instant now,
        @Param("stuckBefore") Instant stuckBefore,
        @Param("batchSize") int batchSize
    );

    List<OutboxEventEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        List<OutboxEventEntity.OutboxEventStatus> statuses,
        Instant now
    );

    List<OutboxEventEntity> findByEmpresaIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
        String empresaId,
        List<OutboxEventEntity.OutboxEventStatus> statuses,
        Instant now
    );

    List<OutboxEventEntity> findByEmpresaIdAndEventTypeOrderByCreatedAtDesc(String empresaId, String eventType);

    List<OutboxEventEntity> findByEmpresaIdAndEventTypeAndStatusOrderByCreatedAtDesc(
        String empresaId,
        String eventType,
        OutboxEventEntity.OutboxEventStatus status
    );

    Optional<OutboxEventEntity> findByIdAndEmpresaId(Long id, String empresaId);

    /**
     * Variante com trava de linha ({@code FOR UPDATE}) para o reprocessamento manual, de modo que
     * ele não corra com o claim do poller sobre o mesmo evento (evita dupla entrega).
     */
    @Query(value = """
        SELECT * FROM outbox_events
        WHERE id = :id AND empresa_id = :empresaId
        FOR UPDATE
        """, nativeQuery = true)
    Optional<OutboxEventEntity> findByIdAndEmpresaIdForUpdate(@Param("id") Long id, @Param("empresaId") String empresaId);
}
