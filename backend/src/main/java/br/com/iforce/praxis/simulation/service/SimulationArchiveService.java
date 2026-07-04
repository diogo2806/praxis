package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;

/**
 * Arquiva avaliações sem apagar histórico operacional.
 *
 * <p>Na visão de produto, esta ação substitui a exclusão definitiva no fluxo de
 * operação do RH. A avaliação sai de uso, mas versões, tentativas, resultados e
 * auditoria permanecem disponíveis para rastreabilidade, LGPD e revisão humana.</p>
 */
@Service
public class SimulationArchiveService {

    private final CurrentEmpresaService currentEmpresaService;
    private final SimulationRepository simulationRepository;
    private final AuditEventService auditEventService;

    public SimulationArchiveService(
            CurrentEmpresaService currentEmpresaService,
            SimulationRepository simulationRepository,
            AuditEventService auditEventService
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.simulationRepository = simulationRepository;
        this.auditEventService = auditEventService;
    }

    /**
     * Arquiva todas as versões da avaliação da empresa logada.
     *
     * @param simulationId identificador da avaliação a retirar de uso
     */
    @Transactional
    public void archiveSimulation(String simulationId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        SimulationEntity simulation = simulationRepository.findByEmpresaIdAndId(empresaId, simulationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos este teste."));

        int archivedVersions = 0;
        for (SimulationVersionEntity version : simulation.getVersions()) {
            if (version.getStatus() != SimulationVersionStatus.ARCHIVED) {
                version.setStatus(SimulationVersionStatus.ARCHIVED);
                archivedVersions++;
            }
        }

        simulationRepository.save(simulation);
        auditEventService.appendSimulationVersionEvent(
                empresaId,
                simulation.getId(),
                latestVersionNumber(simulation),
                AuditEventType.SIMULATION_ARCHIVED,
                "Avaliação arquivada sem exclusão de histórico.",
                "{\"status\":\"archived\",\"archivedVersions\":" + archivedVersions + "}"
        );
    }

    private int latestVersionNumber(SimulationEntity simulation) {
        return simulation.getVersions()
                .stream()
                .max(Comparator.comparingInt(SimulationVersionEntity::getVersionNumber))
                .map(SimulationVersionEntity::getVersionNumber)
                .orElse(1);
    }
}
