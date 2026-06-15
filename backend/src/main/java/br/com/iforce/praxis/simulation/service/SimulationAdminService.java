package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.simulation.dto.PublishSimulationResponse;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
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

    public SimulationAdminService(
            SimulationVersionRepository simulationVersionRepository,
            SimulationValidationService simulationValidationService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.simulationValidationService = simulationValidationService;
    }

    @Transactional(readOnly = true)
    public SimulationValidationResponse validateVersion(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        return simulationValidationService.validate(simulationVersionEntity);
    }

    @Transactional
    public PublishSimulationResponse publishVersion(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        SimulationValidationResponse validationResponse = simulationValidationService.validate(simulationVersionEntity);

        if (!validationResponse.publishable()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Publicacao bloqueada por itens criticos do validador.");
        }

        Instant publishedAt = Instant.now();
        simulationVersionEntity.setStatus(SimulationVersionStatus.PUBLISHED);
        simulationVersionEntity.setPublishedAt(publishedAt);
        SimulationVersionEntity savedSimulationVersionEntity = simulationVersionRepository.save(simulationVersionEntity);

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
}
