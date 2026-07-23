package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.simulation.dto.NormativeGroupRequest;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

/**
 * Impede que uma tentativa de ativação inelegível substitua a referência normativa vigente.
 */
@Service
public class NormativeGroupActivationGuard {

    private final CurrentEmpresaService currentEmpresaService;
    private final SimulationVersionRepository simulationVersionRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;

    public NormativeGroupActivationGuard(
            CurrentEmpresaService currentEmpresaService,
            SimulationVersionRepository simulationVersionRepository,
            CandidateAttemptRepository candidateAttemptRepository
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.simulationVersionRepository = simulationVersionRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
    }

    @Transactional(readOnly = true)
    public void assertEligibleForActivation(
            String simulationId,
            int versionNumber,
            NormativeGroupRequest request
    ) {
        if (!request.activate()) {
            return;
        }

        String empresaId = currentEmpresaService.requiredEmpresaId();
        SimulationVersionEntity version = simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                        empresaId,
                        simulationId,
                        versionNumber
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Versão da avaliação não encontrada."
                ));

        List<CandidateAttemptEntity> population = candidateAttemptRepository
                .findByEmpresaIdAndSimulationVersionIdAndStatus(
                        empresaId,
                        version.getId(),
                        AttemptStatus.COMPLETED
                )
                .stream()
                .filter(attempt -> attempt.getFinishedAt() != null)
                .filter(attempt -> !attempt.getFinishedAt().isBefore(request.periodStart()))
                .filter(attempt -> !attempt.getFinishedAt().isAfter(request.periodEnd()))
                .filter(attempt -> request.gupyJobId() == null
                        || Objects.equals(request.gupyJobId(), attempt.getGupyJobId()))
                .filter(attempt -> attempt.getScore() != null)
                .toList();

        if (!request.pathCompatibilityConfirmed() || population.size() < request.minimumSample()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "O grupo não pode ser ativado: amostra elegível %d, mínimo %d e comparabilidade dos caminhos obrigatória."
                            .formatted(population.size(), request.minimumSample())
            );
        }
    }
}
