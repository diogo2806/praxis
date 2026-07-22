package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

@Service
public class SimulationBranchNodeService {

    private static final double HORIZONTAL_GAP = 350.0;
    private static final double VERTICAL_GAP = 160.0;
    private static final double MIN_POSITION = 24.0;
    private static final double MAX_POSITION_X = 1216.0;
    private static final double MAX_POSITION_Y = 716.0;

    private final SimulationVersionRepository simulationVersionRepository;
    private final CurrentEmpresaService currentEmpresaService;
    private final AuditEventService auditEventService;

    public SimulationBranchNodeService(
            SimulationVersionRepository simulationVersionRepository,
            CurrentEmpresaService currentEmpresaService,
            AuditEventService auditEventService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.auditEventService = auditEventService;
    }

    @Transactional
    public String createBranchNode(
            String simulationId,
            int versionNumber,
            String sourceNodeId,
            String optionId
    ) {
        SimulationVersionEntity version = findDraftVersion(simulationId, versionNumber);
        SimulationNodeEntity sourceNode = findNode(version, sourceNodeId);
        if (sourceNode.isFinal()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Etapas de encerramento não podem criar ramificações."
            );
        }

        SimulationOptionEntity sourceOption = sourceNode.getOptions().stream()
                .filter(option -> Objects.equals(option.getOptionId(), optionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resposta não encontrada."));

        int turnIndex = nextTurnIndex(version);
        String nodeId = nextNodeId(version, turnIndex);
        int branchSlot = branchSlot(sourceNode, sourceOption);

        SimulationNodeEntity newNode = new SimulationNodeEntity();
        newNode.setSimulationVersion(version);
        newNode.setNodeId(nodeId);
        newNode.setTurnIndex(turnIndex);
        newNode.setSpeaker("Cliente");
        newNode.setMessage("");
        newNode.setFinal(false);
        newNode.setPositionX(clamp(sourceX(sourceNode) + HORIZONTAL_GAP, MIN_POSITION, MAX_POSITION_X));
        newNode.setPositionY(clamp(
                sourceY(sourceNode) + branchSlot * VERTICAL_GAP,
                MIN_POSITION,
                MAX_POSITION_Y
        ));

        version.getNodes().add(newNode);
        sourceOption.setNextNodeId(nodeId);
        simulationVersionRepository.save(version);

        auditEventService.appendSimulationVersionEvent(
                version.getSimulation().getEmpresaId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_NODE_ADDED,
                "Etapa ramificada adicionada.",
                "{\"nodeId\":\"" + escapeJson(nodeId)
                        + "\",\"sourceNodeId\":\"" + escapeJson(sourceNodeId)
                        + "\",\"optionId\":\"" + escapeJson(optionId) + "\"}"
        );
        auditEventService.appendSimulationVersionEvent(
                version.getSimulation().getEmpresaId(),
                simulationId,
                versionNumber,
                AuditEventType.SIMULATION_OPTION_UPDATED,
                "Resposta vinculada à nova etapa ramificada.",
                "{\"nodeId\":\"" + escapeJson(sourceNodeId)
                        + "\",\"optionId\":\"" + escapeJson(optionId)
                        + "\",\"nextNodeId\":\"" + escapeJson(nodeId) + "\"}"
        );

        return nodeId;
    }

    private SimulationVersionEntity findDraftVersion(String simulationId, int versionNumber) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        SimulationVersionEntity version = simulationVersionRepository
                .findForBranchCreationByEmpresaIdAndSimulationIdAndVersionNumber(
                        empresaId,
                        simulationId,
                        versionNumber
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Não encontramos esta versão do teste."
                ));
        if (version.getStatus() != SimulationVersionStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Somente versões em rascunho podem criar novas etapas."
            );
        }
        return version;
    }

    private SimulationNodeEntity findNode(SimulationVersionEntity version, String nodeId) {
        return version.getNodes().stream()
                .filter(node -> Objects.equals(node.getNodeId(), nodeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa do teste não encontrada."));
    }

    private int nextTurnIndex(SimulationVersionEntity version) {
        return version.getNodes().stream()
                .mapToInt(SimulationNodeEntity::getTurnIndex)
                .max()
                .orElse(0) + 1;
    }

    private String nextNodeId(SimulationVersionEntity version, int initialIndex) {
        Set<String> nodeIds = new LinkedHashSet<>();
        version.getNodes().forEach(node -> nodeIds.add(node.getNodeId()));
        int index = initialIndex;
        String candidate = "turno-" + index;
        while (nodeIds.contains(candidate)) {
            index += 1;
            candidate = "turno-" + index;
        }
        return candidate;
    }

    private int branchSlot(SimulationNodeEntity sourceNode, SimulationOptionEntity selectedOption) {
        Set<String> existingTargets = new LinkedHashSet<>();
        sourceNode.getOptions().stream()
                .filter(option -> option != selectedOption)
                .map(SimulationOptionEntity::getNextNodeId)
                .filter(Objects::nonNull)
                .forEach(existingTargets::add);
        if (sourceNode.getTimeoutNextNodeId() != null) {
            existingTargets.add(sourceNode.getTimeoutNextNodeId());
        }
        return existingTargets.size();
    }

    private double sourceX(SimulationNodeEntity sourceNode) {
        if (sourceNode.getPositionX() != null) {
            return sourceNode.getPositionX();
        }
        int column = Math.max(0, sourceNode.getTurnIndex() - 1) % 4;
        return 40.0 + column * HORIZONTAL_GAP;
    }

    private double sourceY(SimulationNodeEntity sourceNode) {
        if (sourceNode.getPositionY() != null) {
            return sourceNode.getPositionY();
        }
        int row = Math.max(0, sourceNode.getTurnIndex() - 1) / 4;
        return 40.0 + row * 230.0;
    }

    private double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
