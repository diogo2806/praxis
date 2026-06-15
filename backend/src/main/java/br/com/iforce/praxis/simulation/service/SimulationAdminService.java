package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.simulation.dto.GupyPreflightResponse;
import br.com.iforce.praxis.simulation.dto.PublishSimulationResponse;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.dto.SimulationVersionStatusResponse;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class SimulationAdminService {

    private final SimulationVersionRepository simulationVersionRepository;
    private final SimulationValidationService simulationValidationService;
    private final GupyPreflightService gupyPreflightService;
    private final AuditEventService auditEventService;

    public SimulationAdminService(
            SimulationVersionRepository simulationVersionRepository,
            SimulationValidationService simulationValidationService,
            GupyPreflightService gupyPreflightService,
            AuditEventService auditEventService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.simulationValidationService = simulationValidationService;
        this.gupyPreflightService = gupyPreflightService;
        this.auditEventService = auditEventService;
    }

    @Transactional(readOnly = true)
    public SimulationValidationResponse validateVersion(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        return simulationValidationService.validate(simulationVersionEntity);
    }

    @Transactional
    public SimulationVersionStatusResponse submitVersionForReview(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        requireStatus(
                simulationVersionEntity,
                "Somente versoes em rascunho ou reprovadas podem ir para revisao.",
                SimulationVersionStatus.DRAFT,
                SimulationVersionStatus.REJECTED
        );

        SimulationValidationResponse validationResponse = simulationValidationService.validate(simulationVersionEntity);
        if (!validationResponse.publishable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Revisao bloqueada por itens criticos do validador.");
        }

        simulationVersionEntity.setStatus(SimulationVersionStatus.IN_REVIEW);
        SimulationVersionEntity savedSimulationVersionEntity = simulationVersionRepository.save(simulationVersionEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_SUBMITTED_FOR_REVIEW,
                "Versao de simulacao enviada para revisao.",
                "{\"status\":\"" + savedSimulationVersionEntity.getStatus().getDescricao()
                        + "\",\"warningCount\":" + validationResponse.issues().size() + "}"
        );

        return toStatusResponse(savedSimulationVersionEntity);
    }

    @Transactional
    public SimulationVersionStatusResponse approveVersion(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        requireStatus(
                simulationVersionEntity,
                "Somente versoes em revisao podem ser aprovadas.",
                SimulationVersionStatus.IN_REVIEW
        );

        simulationVersionEntity.setStatus(SimulationVersionStatus.APPROVED);
        SimulationVersionEntity savedSimulationVersionEntity = simulationVersionRepository.save(simulationVersionEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_APPROVED,
                "Versao de simulacao aprovada para publicacao.",
                "{\"status\":\"" + savedSimulationVersionEntity.getStatus().getDescricao() + "\"}"
        );

        return toStatusResponse(savedSimulationVersionEntity);
    }

    @Transactional
    public SimulationVersionStatusResponse rejectVersion(String simulationId, int versionNumber, String reason) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        requireStatus(
                simulationVersionEntity,
                "Somente versoes em revisao podem ser reprovadas.",
                SimulationVersionStatus.IN_REVIEW
        );

        simulationVersionEntity.setStatus(SimulationVersionStatus.REJECTED);
        SimulationVersionEntity savedSimulationVersionEntity = simulationVersionRepository.save(simulationVersionEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_REJECTED,
                "Versao de simulacao reprovada na revisao.",
                "{\"status\":\"" + savedSimulationVersionEntity.getStatus().getDescricao()
                        + "\",\"reason\":\"" + escapeJson(reason) + "\"}"
        );

        return toStatusResponse(savedSimulationVersionEntity);
    }

    @Transactional
    public PublishSimulationResponse publishVersion(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        SimulationValidationResponse validationResponse = simulationValidationService.validate(simulationVersionEntity);

        if (!validationResponse.publishable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Publicacao bloqueada por itens criticos do validador.");
        }

        if (simulationVersionEntity.getStatus() != SimulationVersionStatus.APPROVED
                && simulationVersionEntity.getStatus() != SimulationVersionStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Versao precisa estar aprovada antes da publicacao.");
        }

        GupyPreflightResponse gupyPreflightResponse = gupyPreflightService.evaluate(simulationVersionEntity);
        if (!gupyPreflightResponse.ok()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Preflight Gupy bloqueou a publicacao.");
        }

        Instant publishedAt = Instant.now();
        simulationVersionEntity.setStatus(SimulationVersionStatus.PUBLISHED);
        simulationVersionEntity.setPublishedAt(publishedAt);
        SimulationVersionEntity savedSimulationVersionEntity = simulationVersionRepository.save(simulationVersionEntity);
        auditEventService.appendSimulationVersionEvent(
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                AuditEventType.SIMULATION_VERSION_PUBLISHED,
                "Versao de simulacao publicada.",
                "{\"status\":\"" + savedSimulationVersionEntity.getStatus().getDescricao()
                        + "\",\"publishedAt\":\"" + savedSimulationVersionEntity.getPublishedAt()
                        + "\",\"warningCount\":" + validationResponse.issues().size() + "}"
        );

        return new PublishSimulationResponse(
                savedSimulationVersionEntity.getSimulation().getId(),
                savedSimulationVersionEntity.getVersionNumber(),
                savedSimulationVersionEntity.getStatus(),
                savedSimulationVersionEntity.getPublishedAt()
        );
    }

    private SimulationVersionEntity findVersion(String simulationId, int versionNumber) {
        return simulationVersionRepository.findBySimulationIdAndVersionNumber(simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao de simulacao nao encontrada."));
    }

    private void requireStatus(
            SimulationVersionEntity simulationVersionEntity,
            String message,
            SimulationVersionStatus... allowedStatuses
    ) {
        for (SimulationVersionStatus allowedStatus : allowedStatuses) {
            if (simulationVersionEntity.getStatus() == allowedStatus) {
                return;
            }
        }

        throw new ResponseStatusException(HttpStatus.CONFLICT, message);
    }

    private SimulationVersionStatusResponse toStatusResponse(SimulationVersionEntity simulationVersionEntity) {
        return new SimulationVersionStatusResponse(
                simulationVersionEntity.getSimulation().getId(),
                simulationVersionEntity.getVersionNumber(),
                simulationVersionEntity.getStatus(),
                simulationVersionEntity.getPublishedAt()
        );
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
