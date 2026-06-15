package br.com.iforce.praxis.simulation.persistence.repository;

import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SimulationVersionRepository extends JpaRepository<SimulationVersionEntity, Long> {

    @EntityGraph(attributePaths = {
            "simulation",
            "competencies",
            "nodes",
            "nodes.options",
            "nodes.options.competencyScores"
    })
    List<SimulationVersionEntity> findByStatusOrderByPublishedAtDesc(SimulationVersionStatus status);

    @EntityGraph(attributePaths = {
            "simulation",
            "competencies",
            "nodes",
            "nodes.options",
            "nodes.options.competencyScores"
    })
    Optional<SimulationVersionEntity> findBySimulationIdAndVersionNumber(String simulationId, int versionNumber);

    @EntityGraph(attributePaths = {
            "simulation",
            "competencies",
            "nodes",
            "nodes.options",
            "nodes.options.competencyScores"
    })
    List<SimulationVersionEntity> findBySimulationIdAndStatusOrderByPublishedAtDesc(
            String simulationId,
            SimulationVersionStatus status
    );
}
