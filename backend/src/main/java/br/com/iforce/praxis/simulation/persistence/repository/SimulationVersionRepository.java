package br.com.iforce.praxis.simulation.persistence.repository;

import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    List<SimulationVersionEntity> findByStatusOrderByPublishedAtDesc(
            SimulationVersionStatus status
    );

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
    Optional<SimulationVersionEntity> findById(Long id);

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

    Optional<SimulationVersionEntity> findFirstBySimulationIdOrderByVersionNumberDesc(String simulationId);

    @EntityGraph(attributePaths = {
            "simulation",
            "competencies",
            "nodes",
            "nodes.options",
            "nodes.options.competencyScores"
    })
    Optional<SimulationVersionEntity> findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
            String empresaId,
            String simulationId,
            int versionNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT version
            FROM SimulationVersionEntity version
            WHERE version.id = (
                SELECT candidate.id
                FROM SimulationVersionEntity candidate
                WHERE candidate.simulation.empresaId = :empresaId
                  AND candidate.simulation.id = :simulationId
                  AND candidate.versionNumber = :versionNumber
            )
            """)
    Optional<SimulationVersionEntity> findForBranchCreationByEmpresaIdAndSimulationIdAndVersionNumber(
            @Param("empresaId") String empresaId,
            @Param("simulationId") String simulationId,
            @Param("versionNumber") int versionNumber
    );

    @EntityGraph(attributePaths = {
            "simulation",
            "competencies",
            "nodes",
            "nodes.options",
            "nodes.options.competencyScores"
    })
    List<SimulationVersionEntity> findBySimulationEmpresaIdAndStatusOrderByPublishedAtDesc(
            String empresaId,
            SimulationVersionStatus status
    );

    @EntityGraph(attributePaths = {
            "simulation",
            "competencies",
            "nodes",
            "nodes.options",
            "nodes.options.competencyScores"
    })
    List<SimulationVersionEntity> findBySimulationEmpresaIdAndSimulationIdAndStatusOrderByPublishedAtDesc(
            String empresaId,
            String simulationId,
            SimulationVersionStatus status
    );

    long countBySimulationEmpresaIdAndStatus(String empresaId, SimulationVersionStatus status);
}
