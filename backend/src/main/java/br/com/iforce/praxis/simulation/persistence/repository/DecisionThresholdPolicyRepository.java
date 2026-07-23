package br.com.iforce.praxis.simulation.persistence.repository;

import br.com.iforce.praxis.simulation.model.CutScorePolicyStatus;
import br.com.iforce.praxis.simulation.persistence.entity.DecisionThresholdPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DecisionThresholdPolicyRepository extends JpaRepository<DecisionThresholdPolicyEntity, Long> {

    Optional<DecisionThresholdPolicyEntity> findByIdAndEmpresaId(Long id, String empresaId);

    Optional<DecisionThresholdPolicyEntity> findFirstByEmpresaIdAndSimulationVersionIdAndStatusOrderByUpdatedAtDesc(
            String empresaId,
            Long simulationVersionId,
            CutScorePolicyStatus status
    );

    List<DecisionThresholdPolicyEntity> findByEmpresaIdAndSimulationVersionIdOrderByUpdatedAtDesc(
            String empresaId,
            Long simulationVersionId
    );
}
