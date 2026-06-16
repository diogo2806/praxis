package br.com.iforce.praxis.gupy.delivery.persistence.repository;

import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import br.com.iforce.praxis.gupy.delivery.persistence.entity.ResultDeliveryEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ResultDeliveryRepository extends JpaRepository<ResultDeliveryEntity, Long> {

    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    Optional<ResultDeliveryEntity> findByCandidateAttemptId(String candidateAttemptId);

    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    List<ResultDeliveryEntity> findByStatusOrderByCreatedAtDesc(ResultDeliveryStatus status);

    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    List<ResultDeliveryEntity> findByCandidateAttemptSimulationIdAndCandidateAttemptSimulationVersionNumberOrderByCreatedAtDesc(
            String simulationId,
            Integer simulationVersionNumber
    );

    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    List<ResultDeliveryEntity> findByCandidateAttemptSimulationIdAndCandidateAttemptSimulationVersionNumberAndStatusOrderByCreatedAtDesc(
            String simulationId,
            Integer simulationVersionNumber,
            ResultDeliveryStatus status
    );

    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    List<ResultDeliveryEntity> findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            List<ResultDeliveryStatus> statuses,
            Instant nextAttemptAt
    );

    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    List<ResultDeliveryEntity> findByTenantIdAndStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            String tenantId,
            List<ResultDeliveryStatus> statuses,
            Instant nextAttemptAt
    );

    @Override
    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    Optional<ResultDeliveryEntity> findById(Long id);

    long countByCandidateAttemptSimulationVersionIdAndStatus(
            Long simulationVersionId,
            ResultDeliveryStatus status
    );

    long countByTenantIdAndCandidateAttemptSimulationVersionIdAndStatus(
            String tenantId,
            Long simulationVersionId,
            ResultDeliveryStatus status
    );

    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    List<ResultDeliveryEntity> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    List<ResultDeliveryEntity> findByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, ResultDeliveryStatus status);

    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    List<ResultDeliveryEntity> findByTenantIdAndCandidateAttemptSimulationIdAndCandidateAttemptSimulationVersionNumberOrderByCreatedAtDesc(
            String tenantId,
            String simulationId,
            Integer simulationVersionNumber
    );

    @EntityGraph(attributePaths = {
            "candidateAttempt",
            "candidateAttempt.resultItems"
    })
    List<ResultDeliveryEntity> findByTenantIdAndCandidateAttemptSimulationIdAndCandidateAttemptSimulationVersionNumberAndStatusOrderByCreatedAtDesc(
            String tenantId,
            String simulationId,
            Integer simulationVersionNumber,
            ResultDeliveryStatus status
    );
}
