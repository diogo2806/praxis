package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.candidate.service.BlindMasking;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.simulation.dto.CandidateRadarDto;
import br.com.iforce.praxis.simulation.dto.CandidateReferenceSnapshotDto;
import br.com.iforce.praxis.simulation.dto.CompetencyScoreDto;
import br.com.iforce.praxis.simulation.dto.NormativeReferenceResponse;
import br.com.iforce.praxis.simulation.dto.TalentMatchResponse;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Compara candidatos da mesma versão sem misturar perfil-alvo, referência normativa e nota de corte.
 */
@Service
public class TalentMatchService {

    private static final int MAX_SELECTED_CANDIDATES = 5;

    private final SimulationVersionRepository simulationVersionRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final CurrentEmpresaService currentEmpresaService;
    private final TalentReferenceService talentReferenceService;

    public TalentMatchService(
            SimulationVersionRepository simulationVersionRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            CurrentEmpresaService currentEmpresaService,
            TalentReferenceService talentReferenceService
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.talentReferenceService = talentReferenceService;
    }

    @Transactional
    public TalentMatchResponse getTalentMatch(
            String simulationId,
            int versionNumber,
            List<String> attemptIds,
            boolean blind
    ) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        List<String> normalizedAttemptIds = normalizeAttemptIds(attemptIds);
        SimulationVersionEntity version = simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(empresaId, simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Não encontramos esta versão do teste."
                ));

        List<CandidateAttemptEntity> attempts = candidateAttemptRepository
                .findAllByIdInWithResultItems(normalizedAttemptIds);
        if (attempts.size() != normalizedAttemptIds.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Uma ou mais tentativas não foram encontradas.");
        }

        boolean crossEmpresa = attempts.stream().anyMatch(attempt -> !Objects.equals(attempt.getEmpresaId(), empresaId));
        if (crossEmpresa) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Você não tem acesso a uma ou mais tentativas selecionadas."
            );
        }

        boolean versionMismatch = attempts.stream().anyMatch(attempt ->
                !Objects.equals(attempt.getSimulationId(), simulationId)
                        || !Objects.equals(attempt.getSimulationVersionNumber(), versionNumber)
                        || !Objects.equals(attempt.getSimulationVersionId(), version.getId())
        );
        if (versionMismatch) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Todas as tentativas devem pertencer à mesma versão do teste."
            );
        }

        boolean incomplete = attempts.stream().anyMatch(attempt ->
                attempt.getStatus() != AttemptStatus.COMPLETED || attempt.getScore() == null
        );
        if (incomplete) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Todas as tentativas devem estar concluídas e possuir resultado calculado."
            );
        }

        List<SimulationCompetencyEntity> competencies = version.getCompetencies().stream()
                .sorted(Comparator.comparing(SimulationCompetencyEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        Map<String, CandidateAttemptEntity> attemptsById = new LinkedHashMap<>();
        attempts.forEach(attempt -> attemptsById.put(attempt.getId(), attempt));

        TalentReferenceService.CurrentReferences currentReferences = talentReferenceService
                .currentReferences(version, Instant.now());
        List<String> warnings = new ArrayList<>(currentReferences.warnings());
        List<CandidateRadarDto> candidates = normalizedAttemptIds.stream()
                .map(attemptsById::get)
                .map(attempt -> toCandidateRadar(
                        attempt,
                        version,
                        competencies,
                        blind,
                        currentReferences,
                        warnings
                ))
                .toList();

        return new TalentMatchResponse(
                simulationId,
                versionNumber,
                currentReferences.targetProfile(),
                currentReferences.normativeReference(),
                currentReferences.decisionThreshold(),
                warnings.stream().distinct().toList(),
                candidates
        );
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione no máximo 5 candidatos.");
        }
        return normalized;
    }

    private CandidateRadarDto toCandidateRadar(
            CandidateAttemptEntity attempt,
            SimulationVersionEntity version,
            List<SimulationCompetencyEntity> competencies,
            boolean blind,
            TalentReferenceService.CurrentReferences currentReferences,
            List<String> warnings
    ) {
        Map<String, Integer> scoresByCompetency = new LinkedHashMap<>();
        attempt.getResultItems().stream()
                .sorted(Comparator.comparing(ResultItemEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .forEach(item -> scoresByCompetency.put(item.getName(), item.getScore()));

        List<CompetencyScoreDto> scores = competencies.stream()
                .map(competency -> new CompetencyScoreDto(
                        competency.getName(),
                        scoresByCompetency.getOrDefault(competency.getName(), 0)
                ))
                .toList();
        String candidateName = blind ? BlindMasking.maskedName(attempt.getId()) : attempt.getCandidateName();
        CandidateReferenceSnapshotDto snapshot = talentReferenceService.getOrCreateSnapshot(attempt, version);

        Integer percentile = compatiblePercentile(attempt, snapshot, currentReferences);
        Boolean meetsThreshold = snapshot.decisionThreshold() == null
                ? null
                : attempt.getScore() >= snapshot.decisionThreshold().score();
        appendHistoricalWarnings(attempt, snapshot, currentReferences, warnings);

        return new CandidateRadarDto(
                attempt.getId(),
                candidateName,
                attempt.getScore(),
                percentile,
                meetsThreshold,
                snapshot,
                scores
        );
    }

    private Integer compatiblePercentile(
            CandidateAttemptEntity attempt,
            CandidateReferenceSnapshotDto snapshot,
            TalentReferenceService.CurrentReferences currentReferences
    ) {
        NormativeReferenceResponse snapshotReference = snapshot.normativeReference();
        NormativeReferenceResponse currentReference = currentReferences.normativeReference();
        if (snapshotReference == null || currentReference == null) {
            return null;
        }
        if (!Objects.equals(snapshotReference.id(), currentReference.id())) {
            return null;
        }
        return talentReferenceService.percentile(attempt.getScore(), currentReferences.normativePopulation());
    }

    private void appendHistoricalWarnings(
            CandidateAttemptEntity attempt,
            CandidateReferenceSnapshotDto snapshot,
            TalentReferenceService.CurrentReferences currentReferences,
            List<String> warnings
    ) {
        if (snapshot.normativeReference() != null
                && currentReferences.normativeReference() != null
                && !Objects.equals(snapshot.normativeReference().id(), currentReferences.normativeReference().id())) {
            warnings.add("A tentativa %s preserva uma referência normativa histórica diferente da atual."
                    .formatted(attempt.getId()));
        }
        if (snapshot.decisionThreshold() != null
                && currentReferences.decisionThreshold() != null
                && !Objects.equals(snapshot.decisionThreshold().id(), currentReferences.decisionThreshold().id())) {
            warnings.add("A tentativa %s preserva uma nota de corte histórica diferente da atual."
                    .formatted(attempt.getId()));
        }
    }
}
