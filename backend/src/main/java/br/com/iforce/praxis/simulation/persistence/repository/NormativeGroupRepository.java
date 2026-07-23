package br.com.iforce.praxis.simulation.persistence.repository;

import br.com.iforce.praxis.simulation.model.NormativeGroupStatus;
import br.com.iforce.praxis.simulation.persistence.entity.NormativeGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NormativeGroupRepository extends JpaRepository<NormativeGroupEntity, Long> {

    Optional<NormativeGroupEntity> findByIdAndEmpresaId(Long id, String empresaId);

    Optional<NormativeGroupEntity> findFirstByEmpresaIdAndSimulationVersionIdAndStatusOrderByUpdatedAtDesc(
            String empresaId,
            Long simulationVersionId,
            NormativeGroupStatus status
    );

    List<NormativeGroupEntity> findByEmpresaIdAndSimulationVersionIdOrderByUpdatedAtDesc(
            String empresaId,
            Long simulationVersionId
    );
}
