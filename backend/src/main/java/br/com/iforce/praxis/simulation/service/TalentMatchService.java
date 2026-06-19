package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.simulation.dto.CandidateRadarDto;
import br.com.iforce.praxis.simulation.dto.CompetencyBenchmarkDto;
import br.com.iforce.praxis.simulation.dto.CompetencyScoreDto;
import br.com.iforce.praxis.simulation.dto.TalentMatchResponse;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TalentMatchService {

    private static final int MAX_SELECTED_CANDIDATES = 5;

    private final SimulationVersionRepository simulationVersionRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final CurrentTenantService currentTenantService;

    public TalentMatchService(
            SimulationVersionRepository simulationVersionRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            CurrentTenantService currentTenantService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.currentTenantService = currentTenantService;
    }

    @Transactional(readOnly = true)
    public TalentMatchResponse getTalentMatch(String simulationId, int versionNumber, List<String> attemptIds) {
        String tenantId = currentTenantService.requiredTenantId();
        List<String> normalizedAttemptIds = normalizeAttemptIds(attemptIds);
        SimulationVersionEntity simulationVersionEntity = simulationVersionRepository
                .findBySimulationTenantIdAndSimulationIdAndVersionNumber(tenantId, simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao de simulacao nao encontrada."));

        List<CandidateAttemptEntity> attempts = candidateAttemptRepository.findAllByIdInWithResultItems(normalizedAttemptIds);
        if (attempts.size() != normalizedAttemptIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uma ou mais tentativas nao foram encontradas.");
        }

        boolean crossTenant = attempts.stream().anyMatch(attempt -> !Objects.equals(attempt.getTenantId(), tenantId));
        if (crossTenant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tentativa nao pertence ao tenant autenticado.");
        }

        boolean versionMismatch = attempts.stream().anyMatch(attempt ->
                !Objects.equals(attempt.getSimulationId(), simulationId)
                        || !Objects.equals(attempt.getSimulationVersionNumber(), versionNumber)
                        || !Objects.equals(attempt.getSimulationVersionId(), simulationVersionEntity.getId())
        );
        if (versionMismatch) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todas as tentativas devem pertencer a mesma versao da simulacao.");
        }

        List<SimulationCompetencyEntity> competencies = simulationVersionEntity.getCompetencies().stream()
                .sorted(Comparator.comparing(SimulationCompetencyEntity::getName))
                .toList();
        List<CompetencyBenchmarkDto> benchmark = competencies.stream()
                .map(competency -> new CompetencyBenchmarkDto(competency.getName(), competency.getTargetScore()))
                .toList();

        Map<String, CandidateAttemptEntity> attemptsById = new LinkedHashMap<>();
        attempts.forEach(attempt -> attemptsById.put(attempt.getId(), attempt));

        List<CandidateRadarDto> candidates = normalizedAttemptIds.stream()
                .map(attemptsById::get)
                .map(attempt -> toCandidateRadar(attempt, competencies))
                .toList();

        return new TalentMatchResponse(simulationId, versionNumber, benchmark, candidates);
    }

    private List<String> normalizeAttemptIds(List<String> attemptIds) {
        if (attemptIds == null || attemptIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos uma tentativa.");
        }

        List<String> normalized = attemptIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();

        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos uma tentativa.");
        }
        if (normalized.size() > MAX_SELECTED_CANDIDATES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione no maximo 5 candidatos.");
        }

        return normalized;
    }

    private CandidateRadarDto toCandidateRadar(
            CandidateAttemptEntity attempt,
            List<SimulationCompetencyEntity> competencies
    ) {
        Map<String, Integer> scoresByCompetency = new LinkedHashMap<>();
        attempt.getResultItems().stream()
                .sorted(Comparator.comparing(ResultItemEntity::getName))
                .forEach(item -> scoresByCompetency.put(item.getName(), item.getScore()));

        List<CompetencyScoreDto> scores = competencies.stream()
                .map(competency -> new CompetencyScoreDto(
                        competency.getName(),
                        scoresByCompetency.getOrDefault(competency.getName(), 0)
                ))
                .toList();

        return new CandidateRadarDto(
                attempt.getId(),
                attempt.getCandidateName(),
                attempt.getScore() == null ? 0 : attempt.getScore(),
                scores
        );
    }
}
