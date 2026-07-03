package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.simulation.dto.SimulationMonitoringResponse;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;

import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


/**
 * Calcula os indicadores de acompanhamento de uma versão de prova.
 *
 * <p>Na visão do processo, depois que a prova está no ar, a equipe precisa
 * saber como ela está performando. Este componente conta quantos candidatos
 * receberam a prova e em que situação estão (não iniciada, em andamento,
 * concluída, abandonada, expirada, etc.) e calcula as taxas de conclusão e de
 * desistência, alimentando a tela de monitoramento.</p>
 */
@Service
public class SimulationMonitoringService {

    private final SimulationVersionRepository simulationVersionRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final CurrentEmpresaService currentEmpresaService;

    public SimulationMonitoringService(
            SimulationVersionRepository simulationVersionRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            CurrentEmpresaService currentEmpresaService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.currentEmpresaService = currentEmpresaService;
    }

    /**
     * Reúne os números de acompanhamento de uma versão da prova.
     *
     * <p>Conta as participações por situação e calcula as taxas de conclusão
     * e de desistência (abandono + expiração).</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @return os indicadores de execução da versão
     */
    @Transactional(readOnly = true)
    public SimulationMonitoringResponse getMonitoring(String simulationId, int versionNumber) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        SimulationVersionEntity simulationVersionEntity = simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(empresaId, simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos esta versão do teste."));

        Long simulationVersionId = simulationVersionEntity.getId();
        long attemptsCreated = candidateAttemptRepository.countByEmpresaIdAndSimulationVersionId(empresaId, simulationVersionId);
        long attemptsCompleted = countAttempts(simulationVersionId, AttemptStatus.COMPLETED);
        long attemptsAbandoned = countAttempts(simulationVersionId, AttemptStatus.ABANDONED);
        long attemptsExpired = countAttempts(simulationVersionId, AttemptStatus.EXPIRED);

        return new SimulationMonitoringResponse(
                simulationVersionEntity.getSimulation().getId(),
                simulationVersionEntity.getVersionNumber(),
                attemptsCreated,
                countAttempts(simulationVersionId, AttemptStatus.NOT_STARTED),
                countAttempts(simulationVersionId, AttemptStatus.IN_PROGRESS),
                attemptsCompleted,
                attemptsAbandoned,
                attemptsExpired,
                percent(attemptsCompleted, attemptsCreated),
                percent(attemptsAbandoned + attemptsExpired, attemptsCreated)
        );
    }

    /** Conta quantas participações daquela versão estão em uma dada situação. Uso interno. */
    private long countAttempts(Long simulationVersionId, AttemptStatus status) {
        return candidateAttemptRepository.countByEmpresaIdAndSimulationVersionIdAndStatus(
                currentEmpresaService.requiredEmpresaId(), simulationVersionId, status);
    }

    /** Calcula um percentual com duas casas, tratando o caso de total zero. Uso interno. */
    private double percent(long amount, long total) {
        if (total == 0) {
            return 0.0;
        }

        return Math.round((amount * 10000.0) / total) / 100.0;
    }
}
