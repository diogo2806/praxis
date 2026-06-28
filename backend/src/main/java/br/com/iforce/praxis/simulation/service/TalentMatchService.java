package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.candidate.service.BlindMasking;
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

/**
 * Compara candidatos entre si e contra o perfil ideal da vaga (talent match).
 *
 * <p>Na visão do processo, ajuda o recrutador a decidir comparando até 5
 * candidatos da mesma versão de prova lado a lado: mostra o referencial
 * (benchmark) esperado em cada competência e o desempenho de cada candidato,
 * formando um "radar" por competência. Garante que todos os candidatos
 * pertencem à empresa e à mesma versão, e oferece o modo "às cegas" para
 * ocultar a identificação e reduzir o viés na decisão.</p>
 */
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

    /**
     * Monta o comparativo de candidatos contra o referencial da vaga.
     *
     * <p>Fluxo do processo: valida a seleção (de 1 a 5 candidatos), confere
     * que todos pertencem à empresa e à mesma versão da prova, monta o
     * referencial por competência e o desempenho de cada candidato. No modo
     * "às cegas", a identificação dos candidatos é ocultada.</p>
     *
     * @param simulationId identificador da prova
     * @param versionNumber número da versão
     * @param attemptIds os candidatos (participações) a comparar (no máximo 5)
     * @param blind quando verdadeiro, oculta o nome dos candidatos
     * @return o referencial da vaga e o radar de competências de cada candidato
     */
    @Transactional(readOnly = true)
    public TalentMatchResponse getTalentMatch(String simulationId, int versionNumber, List<String> attemptIds, boolean blind) {
        String tenantId = currentTenantService.requiredTenantId();
        List<String> normalizedAttemptIds = normalizeAttemptIds(attemptIds);
        SimulationVersionEntity simulationVersionEntity = simulationVersionRepository
                .findBySimulationTenantIdAndSimulationIdAndVersionNumber(tenantId, simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos esta versão do teste."));

        List<CandidateAttemptEntity> attempts = candidateAttemptRepository.findAllByIdInWithResultItems(normalizedAttemptIds);
        if (attempts.size() != normalizedAttemptIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uma ou mais tentativas não foram encontradas.");
        }

        boolean crossTenant = attempts.stream().anyMatch(attempt -> !Objects.equals(attempt.getTenantId(), tenantId));
        if (crossTenant) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a uma ou mais tentativas selecionadas.");
        }

        boolean versionMismatch = attempts.stream().anyMatch(attempt ->
                !Objects.equals(attempt.getSimulationId(), simulationId)
                        || !Objects.equals(attempt.getSimulationVersionNumber(), versionNumber)
                        || !Objects.equals(attempt.getSimulationVersionId(), simulationVersionEntity.getId())
        );
        if (versionMismatch) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todas as tentativas devem pertencer à mesma versão do teste.");
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
                .map(attempt -> toCandidateRadar(attempt, competencies, blind))
                .toList();

        return new TalentMatchResponse(simulationId, versionNumber, benchmark, candidates);
    }

    /**
     * Limpa e valida a lista de candidatos selecionados: remove vazios e
     * duplicados e exige de 1 a 5 candidatos. Uso interno.
     */
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione no máximo 5 candidatos.");
        }

        return normalized;
    }

    /**
     * Monta o "radar" de um candidato: seus pontos em cada competência,
     * aplicando o modo às cegas quando solicitado. Uso interno.
     */
    private CandidateRadarDto toCandidateRadar(
            CandidateAttemptEntity attempt,
            List<SimulationCompetencyEntity> competencies,
            boolean blind
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

        // Modo cego: o backend nunca envia o nome no comparativo onde a decisão acontece.
        String candidateName = blind ? BlindMasking.maskedName(attempt.getId()) : attempt.getCandidateName();

        return new CandidateRadarDto(
                attempt.getId(),
                candidateName,
                attempt.getScore() == null ? 0 : attempt.getScore(),
                scores
        );
    }
}
