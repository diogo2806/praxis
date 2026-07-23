package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.simulation.dto.CandidateReferenceSnapshotDto;
import br.com.iforce.praxis.simulation.dto.CompetencyTargetProfileDto;
import br.com.iforce.praxis.simulation.dto.DecisionThresholdRequest;
import br.com.iforce.praxis.simulation.dto.DecisionThresholdResponse;
import br.com.iforce.praxis.simulation.dto.NormativeGroupRequest;
import br.com.iforce.praxis.simulation.dto.NormativeMetricDto;
import br.com.iforce.praxis.simulation.dto.NormativeReferenceResponse;
import br.com.iforce.praxis.simulation.dto.TalentReferenceConfigurationResponse;
import br.com.iforce.praxis.simulation.model.CutScorePolicyStatus;
import br.com.iforce.praxis.simulation.model.NormativeGroupStatus;
import br.com.iforce.praxis.simulation.persistence.entity.DecisionThresholdPolicyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.NormativeGroupEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.TalentReferenceSnapshotEntity;
import br.com.iforce.praxis.simulation.persistence.repository.DecisionThresholdPolicyRepository;
import br.com.iforce.praxis.simulation.persistence.repository.NormativeGroupRepository;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import br.com.iforce.praxis.simulation.persistence.repository.TalentReferenceSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Optional;

@Service
public class TalentReferenceService {

    public static final String TARGET_PROFILE_SOURCE = "CONFIGURED_TARGET_PROFILE";
    public static final String TARGET_PROFILE_WARNING =
            "Perfil desejado configurado pela organização; não é média normativa nem garantia de desempenho.";
    public static final String NO_NORMATIVE_REFERENCE_WARNING =
            "Sem referência normativa elegível. Média e percentil permanecem ocultos.";
    public static final String NO_DECISION_THRESHOLD_WARNING =
            "Sem nota de corte aprovada para esta versão. Nenhuma recomendação binária será produzida.";

    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final SimulationVersionRepository simulationVersionRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final NormativeGroupRepository normativeGroupRepository;
    private final DecisionThresholdPolicyRepository decisionThresholdPolicyRepository;
    private final TalentReferenceSnapshotRepository snapshotRepository;
    private final AuditEventService auditEventService;
    private final ObjectMapper objectMapper;

    public TalentReferenceService(
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            SimulationVersionRepository simulationVersionRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            NormativeGroupRepository normativeGroupRepository,
            DecisionThresholdPolicyRepository decisionThresholdPolicyRepository,
            TalentReferenceSnapshotRepository snapshotRepository,
            AuditEventService auditEventService,
            ObjectMapper objectMapper
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.simulationVersionRepository = simulationVersionRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.normativeGroupRepository = normativeGroupRepository;
        this.decisionThresholdPolicyRepository = decisionThresholdPolicyRepository;
        this.snapshotRepository = snapshotRepository;
        this.auditEventService = auditEventService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public NormativeReferenceResponse configureNormativeGroup(
            String simulationId,
            int versionNumber,
            NormativeGroupRequest request
    ) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String userId = currentUserService.requiredUserId();
        SimulationVersionEntity version = loadVersion(empresaId, simulationId, versionNumber);
        validatePeriod(request.periodStart(), request.periodEnd(), "O período do grupo normativo é inválido.");

        List<CandidateAttemptEntity> population = normativePopulation(empresaId, version, request);
        boolean eligible = population.size() >= request.minimumSample() && request.pathCompatibilityConfirmed();

        if (request.activate()) {
            normativeGroupRepository
                    .findFirstByEmpresaIdAndSimulationVersionIdAndStatusOrderByUpdatedAtDesc(
                            empresaId,
                            version.getId(),
                            NormativeGroupStatus.ACTIVE
                    )
                    .ifPresent(previous -> {
                        previous.setStatus(NormativeGroupStatus.ARCHIVED);
                        previous.setUpdatedAt(Instant.now());
                        normativeGroupRepository.save(previous);
                    });
        }

        Instant now = Instant.now();
        NormativeGroupEntity entity = new NormativeGroupEntity();
        entity.setEmpresaId(empresaId);
        entity.setSimulationId(simulationId);
        entity.setSimulationVersion(version);
        entity.setVersionNumber(versionNumber);
        entity.setName(request.name().trim());
        entity.setJobTitle(request.jobTitle().trim());
        entity.setSeniority(trimToNull(request.seniority()));
        entity.setGupyJobId(request.gupyJobId());
        entity.setPopulationDescription(request.populationDescription().trim());
        entity.setPeriodStart(request.periodStart());
        entity.setPeriodEnd(request.periodEnd());
        entity.setMinimumSample(request.minimumSample());
        entity.setPathCompatibilityConfirmed(request.pathCompatibilityConfirmed());
        entity.setStatus(request.activate()
                ? (eligible ? NormativeGroupStatus.ACTIVE : NormativeGroupStatus.INELIGIBLE)
                : NormativeGroupStatus.DRAFT);
        entity.setCreatedBy(userId);
        entity.setApprovedBy(entity.getStatus() == NormativeGroupStatus.ACTIVE ? userId : null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        NormativeGroupEntity saved = normativeGroupRepository.save(entity);

        auditEventService.appendSimulationVersionEvent(
                empresaId,
                simulationId,
                versionNumber,
                AuditEventType.TALENT_NORMATIVE_GROUP_CONFIGURED,
                "Grupo normativo configurado para o Talent Match.",
                metadata(Map.of(
                        "normativeGroupId", saved.getId(),
                        "status", saved.getStatus().name(),
                        "sampleSize", population.size(),
                        "minimumSample", saved.getMinimumSample(),
                        "gupyJobId", Objects.toString(saved.getGupyJobId(), "")
                ))
        );
        return toNormativeResponse(saved, population);
    }

    @Transactional
    public DecisionThresholdResponse configureDecisionThreshold(
            String simulationId,
            int versionNumber,
            DecisionThresholdRequest request
    ) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String userId = currentUserService.requiredUserId();
        SimulationVersionEntity version = loadVersion(empresaId, simulationId, versionNumber);
        if (request.validUntil() != null) {
            validatePeriod(request.validFrom(), request.validUntil(), "A validade da nota de corte é inválida.");
        }

        if (request.approve()) {
            decisionThresholdPolicyRepository
                    .findFirstByEmpresaIdAndSimulationVersionIdAndStatusOrderByUpdatedAtDesc(
                            empresaId,
                            version.getId(),
                            CutScorePolicyStatus.APPROVED
                    )
                    .ifPresent(previous -> {
                        previous.setStatus(CutScorePolicyStatus.REVOKED);
                        previous.setUpdatedAt(Instant.now());
                        decisionThresholdPolicyRepository.save(previous);
                    });
        }

        Instant now = Instant.now();
        DecisionThresholdPolicyEntity entity = new DecisionThresholdPolicyEntity();
        entity.setEmpresaId(empresaId);
        entity.setSimulationId(simulationId);
        entity.setSimulationVersion(version);
        entity.setVersionNumber(versionNumber);
        entity.setScore(request.score());
        entity.setPopulationDescription(request.populationDescription().trim());
        entity.setJustification(request.justification().trim());
        entity.setEvidence(request.evidence().trim());
        entity.setValidFrom(request.validFrom());
        entity.setValidUntil(request.validUntil());
        entity.setStatus(request.approve() ? CutScorePolicyStatus.APPROVED : CutScorePolicyStatus.DRAFT);
        entity.setCreatedBy(userId);
        entity.setApprovedBy(request.approve() ? userId : null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        DecisionThresholdPolicyEntity saved = decisionThresholdPolicyRepository.save(entity);

        auditEventService.appendSimulationVersionEvent(
                empresaId,
                simulationId,
                versionNumber,
                AuditEventType.TALENT_DECISION_THRESHOLD_CONFIGURED,
                "Nota de corte configurada para o Talent Match.",
                metadata(Map.of(
                        "decisionThresholdId", saved.getId(),
                        "score", saved.getScore(),
                        "status", saved.getStatus().name(),
                        "validFrom", saved.getValidFrom().toString()
                ))
        );
        return toDecisionThresholdResponse(saved, Instant.now());
    }

    @Transactional(readOnly = true)
    public TalentReferenceConfigurationResponse getConfiguration(String simulationId, int versionNumber) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        SimulationVersionEntity version = loadVersion(empresaId, simulationId, versionNumber);
        List<NormativeReferenceResponse> normativeGroups = normativeGroupRepository
                .findByEmpresaIdAndSimulationVersionIdOrderByUpdatedAtDesc(empresaId, version.getId())
                .stream()
                .map(group -> toNormativeResponse(group, normativePopulation(empresaId, version, group)))
                .toList();
        List<DecisionThresholdResponse> thresholds = decisionThresholdPolicyRepository
                .findByEmpresaIdAndSimulationVersionIdOrderByUpdatedAtDesc(empresaId, version.getId())
                .stream()
                .map(policy -> toDecisionThresholdResponse(policy, Instant.now()))
                .toList();
        return new TalentReferenceConfigurationResponse(
                targetProfile(version),
                normativeGroups,
                thresholds,
                configurationWarnings(normativeGroups, thresholds)
        );
    }

    @Transactional(readOnly = true)
    public CurrentReferences currentReferences(SimulationVersionEntity version, Instant referenceAt) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        List<CompetencyTargetProfileDto> targetProfile = targetProfile(version);

        Optional<NormativeGroupEntity> activeGroup = normativeGroupRepository
                .findFirstByEmpresaIdAndSimulationVersionIdAndStatusOrderByUpdatedAtDesc(
                        empresaId,
                        version.getId(),
                        NormativeGroupStatus.ACTIVE
                );
        List<CandidateAttemptEntity> population = activeGroup
                .map(group -> normativePopulation(empresaId, version, group))
                .orElseGet(List::of);
        NormativeReferenceResponse normative = activeGroup
                .map(group -> toNormativeResponse(group, population))
                .filter(NormativeReferenceResponse::eligible)
                .orElse(null);

        DecisionThresholdResponse threshold = decisionThresholdPolicyRepository
                .findFirstByEmpresaIdAndSimulationVersionIdAndStatusOrderByUpdatedAtDesc(
                        empresaId,
                        version.getId(),
                        CutScorePolicyStatus.APPROVED
                )
                .map(policy -> toDecisionThresholdResponse(policy, referenceAt))
                .filter(response -> response.warning() == null)
                .orElse(null);

        List<String> warnings = new ArrayList<>();
        warnings.add(TARGET_PROFILE_WARNING);
        if (normative == null) {
            warnings.add(NO_NORMATIVE_REFERENCE_WARNING);
        }
        if (threshold == null) {
            warnings.add(NO_DECISION_THRESHOLD_WARNING);
        }
        return new CurrentReferences(targetProfile, normative, threshold, population, List.copyOf(warnings));
    }

    @Transactional
    public CandidateReferenceSnapshotDto getOrCreateSnapshot(
            CandidateAttemptEntity attempt,
            SimulationVersionEntity version
    ) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        Optional<TalentReferenceSnapshotEntity> existing = snapshotRepository
                .findByEmpresaIdAndAttemptId(empresaId, attempt.getId());
        if (existing.isPresent()) {
            return readSnapshot(existing.get());
        }

        Instant referenceAt = attempt.getFinishedAt() == null ? Instant.now() : attempt.getFinishedAt();
        List<CompetencyTargetProfileDto> targetProfile = targetProfile(version);
        NormativeReferenceResponse normative = normativeReferenceAt(empresaId, version, referenceAt);
        DecisionThresholdResponse threshold = decisionThresholdAt(empresaId, version, referenceAt);

        TalentReferenceSnapshotEntity snapshot = new TalentReferenceSnapshotEntity();
        snapshot.setEmpresaId(empresaId);
        snapshot.setAttemptId(attempt.getId());
        snapshot.setSimulationId(attempt.getSimulationId());
        snapshot.setSimulationVersion(version);
        snapshot.setVersionNumber(version.getVersionNumber());
        snapshot.setTargetProfileJson(writeJson(targetProfile));
        snapshot.setNormativeReferenceJson(normative == null ? null : writeJson(normative));
        snapshot.setDecisionThresholdJson(threshold == null ? null : writeJson(threshold));
        snapshot.setCapturedAt(Instant.now());
        TalentReferenceSnapshotEntity saved = snapshotRepository.save(snapshot);

        auditEventService.appendCandidateAttemptEvent(
                empresaId,
                attempt.getId(),
                AuditEventType.TALENT_REFERENCE_SNAPSHOT_CAPTURED,
                "Referências do Talent Match preservadas para o histórico da tentativa.",
                metadata(Map.of(
                        "simulationId", attempt.getSimulationId(),
                        "versionNumber", version.getVersionNumber(),
                        "normativeReference", normative != null,
                        "decisionThreshold", threshold != null,
                        "referenceAt", referenceAt.toString()
                ))
        );
        return readSnapshot(saved);
    }

    public int percentile(int score, List<CandidateAttemptEntity> population) {
        List<Integer> validScores = population.stream()
                .map(CandidateAttemptEntity::getScore)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (validScores.isEmpty()) {
            return 0;
        }
        long belowOrEqual = validScores.stream().filter(value -> value <= score).count();
        return (int) Math.round((belowOrEqual * 100.0) / validScores.size());
    }

    private NormativeReferenceResponse normativeReferenceAt(
            String empresaId,
            SimulationVersionEntity version,
            Instant referenceAt
    ) {
        return normativeGroupRepository
                .findByEmpresaIdAndSimulationVersionIdOrderByUpdatedAtDesc(empresaId, version.getId())
                .stream()
                .filter(group -> group.getCreatedAt() != null && !group.getCreatedAt().isAfter(referenceAt))
                .filter(group -> group.getStatus() == NormativeGroupStatus.ACTIVE
                        || group.getStatus() == NormativeGroupStatus.ARCHIVED)
                .filter(group -> !referenceAt.isBefore(group.getPeriodStart()) && !referenceAt.isAfter(group.getPeriodEnd()))
                .map(group -> toNormativeResponse(group, normativePopulation(empresaId, version, group)))
                .filter(NormativeReferenceResponse::eligible)
                .findFirst()
                .orElse(null);
    }

    private DecisionThresholdResponse decisionThresholdAt(
            String empresaId,
            SimulationVersionEntity version,
            Instant referenceAt
    ) {
        return decisionThresholdPolicyRepository
                .findByEmpresaIdAndSimulationVersionIdOrderByUpdatedAtDesc(empresaId, version.getId())
                .stream()
                .filter(policy -> policy.getCreatedAt() != null && !policy.getCreatedAt().isAfter(referenceAt))
                .filter(policy -> policy.getStatus() == CutScorePolicyStatus.APPROVED
                        || policy.getStatus() == CutScorePolicyStatus.REVOKED
                        || policy.getStatus() == CutScorePolicyStatus.EXPIRED)
                .map(policy -> toDecisionThresholdResponse(policy, referenceAt))
                .filter(response -> response.warning() == null)
                .findFirst()
                .orElse(null);
    }

    private List<CandidateAttemptEntity> normativePopulation(
            String empresaId,
            SimulationVersionEntity version,
            NormativeGroupRequest request
    ) {
        return filterPopulation(
                candidateAttemptRepository.findByEmpresaIdAndSimulationVersionIdAndStatus(
                        empresaId,
                        version.getId(),
                        AttemptStatus.DONE
                ),
                request.periodStart(),
                request.periodEnd(),
                request.gupyJobId()
        );
    }

    private List<CandidateAttemptEntity> normativePopulation(
            String empresaId,
            SimulationVersionEntity version,
            NormativeGroupEntity group
    ) {
        return filterPopulation(
                candidateAttemptRepository.findByEmpresaIdAndSimulationVersionIdAndStatus(
                        empresaId,
                        version.getId(),
                        AttemptStatus.DONE
                ),
                group.getPeriodStart(),
                group.getPeriodEnd(),
                group.getGupyJobId()
        );
    }

    private List<CandidateAttemptEntity> filterPopulation(
            List<CandidateAttemptEntity> attempts,
            Instant periodStart,
            Instant periodEnd,
            Long gupyJobId
    ) {
        return attempts.stream()
                .filter(attempt -> attempt.getFinishedAt() != null)
                .filter(attempt -> !attempt.getFinishedAt().isBefore(periodStart))
                .filter(attempt -> !attempt.getFinishedAt().isAfter(periodEnd))
                .filter(attempt -> gupyJobId == null || Objects.equals(gupyJobId, attempt.getGupyJobId()))
                .filter(attempt -> attempt.getScore() != null)
                .toList();
    }

    private NormativeReferenceResponse toNormativeResponse(
            NormativeGroupEntity group,
            List<CandidateAttemptEntity> population
    ) {
        boolean eligible = group.isPathCompatibilityConfirmed()
                && population.size() >= group.getMinimumSample()
                && (group.getStatus() == NormativeGroupStatus.ACTIVE
                || group.getStatus() == NormativeGroupStatus.ARCHIVED);
        String limitation = eligible
                ? null
                : "A referência não atende à amostra mínima e à confirmação de comparabilidade dos caminhos.";
        return new NormativeReferenceResponse(
                group.getId(),
                group.getName(),
                group.getJobTitle(),
                group.getSeniority(),
                group.getGupyJobId(),
                group.getPopulationDescription(),
                group.getPeriodStart(),
                group.getPeriodEnd(),
                group.getVersionNumber(),
                population.size(),
                group.getMinimumSample(),
                eligible,
                group.getStatus(),
                limitation,
                eligible ? normativeMetrics(population) : List.of()
        );
    }

    private List<NormativeMetricDto> normativeMetrics(List<CandidateAttemptEntity> population) {
        Map<String, List<Integer>> scoresByCompetency = new LinkedHashMap<>();
        for (CandidateAttemptEntity attempt : population) {
            for (ResultItemEntity item : attempt.getResultItems()) {
                scoresByCompetency.computeIfAbsent(item.getName(), ignored -> new ArrayList<>()).add(item.getScore());
            }
        }
        return scoresByCompetency.entrySet().stream()
                .map(entry -> metric(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(NormativeMetricDto::competencyName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private NormativeMetricDto metric(String competencyName, List<Integer> scores) {
        double mean = scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = scores.stream()
                .mapToDouble(score -> Math.pow(score - mean, 2))
                .average()
                .orElse(0.0);
        return new NormativeMetricDto(
                competencyName,
                scores.size(),
                round(mean),
                round(Math.sqrt(variance))
        );
    }

    private List<CompetencyTargetProfileDto> targetProfile(SimulationVersionEntity version) {
        return version.getCompetencies().stream()
                .map(competency -> new CompetencyTargetProfileDto(
                        competency.getName(),
                        competency.getTargetScore(),
                        TARGET_PROFILE_SOURCE,
                        TARGET_PROFILE_WARNING
                ))
                .sorted(Comparator.comparing(CompetencyTargetProfileDto::competencyName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private DecisionThresholdResponse toDecisionThresholdResponse(
            DecisionThresholdPolicyEntity entity,
            Instant referenceAt
    ) {
        String warning = null;
        if (referenceAt.isBefore(entity.getValidFrom())) {
            warning = "A nota de corte ainda não está vigente.";
        } else if (entity.getValidUntil() != null && referenceAt.isAfter(entity.getValidUntil())) {
            warning = "A nota de corte não estava vigente na data de referência.";
        } else if (entity.getStatus() != CutScorePolicyStatus.APPROVED
                && entity.getStatus() != CutScorePolicyStatus.REVOKED
                && entity.getStatus() != CutScorePolicyStatus.EXPIRED) {
            warning = "A nota de corte ainda não foi aprovada.";
        }
        return new DecisionThresholdResponse(
                entity.getId(),
                entity.getScore(),
                entity.getPopulationDescription(),
                entity.getJustification(),
                entity.getEvidence(),
                entity.getValidFrom(),
                entity.getValidUntil(),
                entity.getStatus(),
                entity.getApprovedBy(),
                warning
        );
    }

    private List<String> configurationWarnings(
            List<NormativeReferenceResponse> normativeGroups,
            List<DecisionThresholdResponse> thresholds
    ) {
        List<String> warnings = new ArrayList<>();
        warnings.add(TARGET_PROFILE_WARNING);
        if (normativeGroups.stream().noneMatch(group -> group.status() == NormativeGroupStatus.ACTIVE && group.eligible())) {
            warnings.add(NO_NORMATIVE_REFERENCE_WARNING);
        }
        if (thresholds.stream().noneMatch(threshold -> threshold.status() == CutScorePolicyStatus.APPROVED
                && threshold.warning() == null)) {
            warnings.add(NO_DECISION_THRESHOLD_WARNING);
        }
        return List.copyOf(warnings);
    }

    private SimulationVersionEntity loadVersion(String empresaId, String simulationId, int versionNumber) {
        return simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(empresaId, simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão da avaliação não encontrada."));
    }

    private CandidateReferenceSnapshotDto readSnapshot(TalentReferenceSnapshotEntity entity) {
        try {
            List<CompetencyTargetProfileDto> targetProfile = objectMapper.readValue(
                    entity.getTargetProfileJson(),
                    new TypeReference<>() { }
            );
            NormativeReferenceResponse normative = entity.getNormativeReferenceJson() == null
                    ? null
                    : objectMapper.readValue(entity.getNormativeReferenceJson(), NormativeReferenceResponse.class);
            DecisionThresholdResponse threshold = entity.getDecisionThresholdJson() == null
                    ? null
                    : objectMapper.readValue(entity.getDecisionThresholdJson(), DecisionThresholdResponse.class);
            return new CandidateReferenceSnapshotDto(targetProfile, normative, threshold, entity.getCapturedAt());
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao recuperar o histórico de referências do Talent Match.",
                    exception
            );
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Falha ao preservar as referências do Talent Match.",
                    exception
            );
        }
    }

    private String metadata(Map<String, ?> value) {
        return writeJson(value);
    }

    private void validatePeriod(Instant start, Instant end, String message) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public record CurrentReferences(
            List<CompetencyTargetProfileDto> targetProfile,
            NormativeReferenceResponse normativeReference,
            DecisionThresholdResponse decisionThreshold,
            List<CandidateAttemptEntity> normativePopulation,
            List<String> warnings
    ) {
    }
}
