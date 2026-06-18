package br.com.iforce.praxis.simulation.persistence.repository;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimulationRepository extends JpaRepository<SimulationEntity, String> {

    @EntityGraph(attributePaths = {
            "versions",
            "versions.competencies"
    })
    List<SimulationEntity> findByArchivedFalseAndDeletedAtIsNullOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {
            "versions",
            "versions.competencies"
    })
    List<SimulationEntity> findByTenantIdAndArchivedFalseAndDeletedAtIsNullOrderByCreatedAtDesc(String tenantId);

    Optional<SimulationEntity> findByTenantIdAndId(String tenantId, String id);
}
