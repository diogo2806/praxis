package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import br.com.iforce.praxis.gupy.delivery.persistence.repository.ResultDeliveryRepository;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.simulation.dto.SimulationMonitoringResponse;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SimulationMonitoringService {

    private final SimulationVersionRepository simulationVersionRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final ResultDeliveryRepository resultDeliveryRepository;
    private final CurrentTenantService currentTenantService;

    public SimulationMonitoringService(
            SimulationVersionRepository simulationVersionRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            ResultDeliveryRepository resultDeliveryRepository,
            CurrentTenantService currentTenantService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.resultDeliveryRepository = resultDeliveryRepository;
        this.currentTenantService = currentTenantService;
    }

    @Transactional(readOnly = true)
    public SimulationMonitoringResponse getMonitoring(String simulationId, int versionNumber) {
        String tenantId = currentTenantService.requiredTenantId();
        SimulationVersionEntity simulationVersionEntity = simulationVersionRepository
                .findBySimulationTenantIdAndSimulationIdAndVersionNumber(tenantId, simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao de simulacao nao encontrada."));

        Long simulationVersionId = simulationVersionEntity.getId();
        long attemptsCreated = candidateAttemptRepository.countByTenantIdAndSimulationVersionId(tenantId, simulationVersionId);
        long attemptsCompleted = countAttempts(simulationVersionId, AttemptStatus.COMPLETED);
        long attemptsAbandoned = countAttempts(simulationVersionId, AttemptStatus.ABANDONED);
        long attemptsExpired = countAttempts(simulationVersionId, AttemptStatus.EXPIRED);

        return new SimulationMonitoringResponse(
                simulationVersionEntity.getSimulation().getId(),
                simulationVersionEntity.getVersionNumber(),
                attemptsCreated,
                countAttempts(simulationVersionId, AttemptStatus.NOT_STARTED),
                countAttempts(simulationVersionId, AttemptStatus.IN_PROGRESS),
                countAttempts(simulationVersionId, AttemptStatus.PAUSED),
                attemptsCompleted,
                attemptsAbandoned,
                attemptsExpired,
                countAttempts(simulationVersionId, AttemptStatus.FAILED),
                percent(attemptsCompleted, attemptsCreated),
                percent(attemptsAbandoned + attemptsExpired, attemptsCreated),
                countDeliveries(simulationVersionId, ResultDeliveryStatus.PENDING),
                countDeliveries(simulationVersionId, ResultDeliveryStatus.RETRYING),
                countDeliveries(simulationVersionId, ResultDeliveryStatus.SENT),
                countDeliveries(simulationVersionId, ResultDeliveryStatus.DLQ)
        );
    }

    private long countAttempts(Long simulationVersionId, AttemptStatus status) {
        return candidateAttemptRepository.countByTenantIdAndSimulationVersionIdAndStatus(
                currentTenantService.requiredTenantId(), simulationVersionId, status);
    }

    private long countDeliveries(Long simulationVersionId, ResultDeliveryStatus status) {
        return resultDeliveryRepository.countByTenantIdAndCandidateAttemptSimulationVersionIdAndStatus(
                currentTenantService.requiredTenantId(), simulationVersionId, status);
    }

    private double percent(long amount, long total) {
        if (total == 0) {
            return 0.0;
        }

        return Math.round((amount * 10000.0) / total) / 100.0;
    }
}
