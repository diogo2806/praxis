package br.com.iforce.praxis.answerkey.service;

import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.AssignmentRequest;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.AssignmentResponse;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.AssignmentRole;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.CreateRoundRequest;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.EvidenceRequest;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.EvidenceResponse;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.OptionConsensusResponse;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.OptionReviewRequest;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.ReviewEventResponse;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.ReviewSummaryResponse;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.RoundResponse;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.OptionCompetencyScoreEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class AnswerKeyReviewService {

    private static final int DEFAULT_MINIMUM_EXPERTS = 2;
    private static final BigDecimal DEFAULT_MINIMUM_CONSENSUS = new BigDecimal("0.7000");
    private static final BigDecimal AMBIGUITY_DISPERSION = new BigDecimal("15.0000");

    private final JdbcTemplate jdbcTemplate;
    private final SimulationVersionRepository simulationVersionRepository;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;

    public AnswerKeyReviewService(
            JdbcTemplate jdbcTemplate,
            SimulationVersionRepository simulationVersionRepository,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            ObjectMapper objectMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.simulationVersionRepository = simulationVersionRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public RoundResponse createRound(String simulationId, int versionNumber, CreateRoundRequest request) {
        SimulationVersionEntity version = loadVersion(simulationId, versionNumber);
        assertDraft(version);

        int minimumExperts = request.minimumExperts() == null
                ? DEFAULT_MINIMUM_EXPERTS
                : request.minimumExperts();
        BigDecimal minimumConsensus = request.minimumConsensus() == null
                ? DEFAULT_MINIMUM_CONSENSUS
                : request.minimumConsensus().setScale(4, RoundingMode.HALF_UP);
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actor = currentUserService.requiredUserId();
        int roundNumber = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(MAX(round_number), 0) + 1
                        FROM answer_key_review_rounds
                        WHERE empresa_id = ? AND simulation_id = ? AND version_number = ?
                        """,
                Integer.class,
                empresaId,
                simulationId,
                versionNumber
        );

        UUID roundId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                        INSERT INTO answer_key_review_rounds (
                            id, empresa_id, simulation_id, version_number, round_number, status,
                            minimum_experts, minimum_consensus, created_by, created_at
                        ) VALUES (?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?)
                        """,
                roundId,
                empresaId,
                simulationId,
                versionNumber,
                roundNumber,
                minimumExperts,
                minimumConsensus,
                actor,
                now
        );
        recordEvent(roundId, "ROUND_CREATED", actor, Map.of(
                "minimumExperts", minimumExperts,
                "minimumConsensus", minimumConsensus
        ));
        return findRound(roundId, simulationId, versionNumber).toResponse();
    }

    @Transactional
    public ReviewSummaryResponse invite(
            String simulationId,
            int versionNumber,
            UUID roundId,
            AssignmentRequest request
    ) {
        ReviewRound round = findRound(roundId, simulationId, versionNumber);
        assertMutable(round);
        String actor = currentUserService.requiredUserId();
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                        INSERT INTO answer_key_review_assignments (
                            round_id, user_id, assignment_role, status, invited_by, invited_at
                        ) VALUES (?, ?, ?, 'INVITED', ?, ?)
                        ON CONFLICT (round_id, user_id, assignment_role)
                        DO UPDATE SET invited_by = EXCLUDED.invited_by, invited_at = EXCLUDED.invited_at
                        """,
                roundId,
                request.userId().trim(),
                request.role().name(),
                actor,
                now
        );
        jdbcTemplate.update(
                "UPDATE answer_key_review_rounds SET status = 'IN_REVIEW' WHERE id = ? AND status = 'DRAFT'",
                roundId
        );
        recordEvent(roundId, "ASSIGNMENT_INVITED", actor, Map.of(
                "userId", request.userId().trim(),
                "role", request.role().name()
        ));
        return getSummary(simulationId, versionNumber, roundId);
    }

    @Transactional
    public ReviewSummaryResponse saveEvidence(
            String simulationId,
            int versionNumber,
            UUID roundId,
            String nodeId,
            EvidenceRequest request
    ) {
        ReviewRound round = findRound(roundId, simulationId, versionNumber);
        assertMutable(round);
        SimulationVersionEntity version = loadVersion(simulationId, versionNumber);
        SimulationNodeEntity node = findNode(version, nodeId);
        if (node.isFinal()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Etapas finais não recebem matriz de evidências.");
        }
        boolean competencyExists = version.getCompetencies().stream()
                .anyMatch(competency -> competency.getName().equalsIgnoreCase(request.competency().trim()));
        if (!competencyExists) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A competência informada não pertence à versão.");
        }

        String actor = currentUserService.requiredUserId();
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                        INSERT INTO answer_key_evidence_links (
                            round_id, node_id, task_text, risk_text, competency_name, indicator,
                            evidence_weight, created_by, created_at, updated_by, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (round_id, node_id, competency_name, indicator)
                        DO UPDATE SET
                            task_text = EXCLUDED.task_text,
                            risk_text = EXCLUDED.risk_text,
                            evidence_weight = EXCLUDED.evidence_weight,
                            updated_by = EXCLUDED.updated_by,
                            updated_at = EXCLUDED.updated_at
                        """,
                roundId,
                nodeId,
                request.task().trim(),
                request.risk().trim(),
                request.competency().trim(),
                request.indicator().trim(),
                request.weight(),
                actor,
                now,
                actor,
                now
        );
        recordEvent(roundId, "EVIDENCE_UPDATED", actor, Map.of(
                "nodeId", nodeId,
                "competency", request.competency().trim(),
                "indicator", request.indicator().trim()
        ));
        return getSummary(simulationId, versionNumber, roundId);
    }

    @Transactional
    public ReviewSummaryResponse reviewOption(
            String simulationId,
            int versionNumber,
            UUID roundId,
            String nodeId,
            String optionId,
            OptionReviewRequest request
    ) {
        ReviewRound round = findRound(roundId, simulationId, versionNumber);
        assertMutable(round);
        String reviewer = currentUserService.requiredUserId();
        requireAssignment(roundId, reviewer, AssignmentRole.EXPERT);

        SimulationVersionEntity version = loadVersion(simulationId, versionNumber);
        SimulationOptionEntity option = findOption(version, nodeId, optionId);
        validateCompetencyScores(version, request.competencyScores());
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                        INSERT INTO answer_key_option_reviews (
                            round_id, reviewer_user_id, node_id, option_id, effectiveness_score,
                            behavioral_justification, competency_scores_json, created_at, updated_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        ON CONFLICT (round_id, reviewer_user_id, node_id, option_id)
                        DO UPDATE SET
                            effectiveness_score = EXCLUDED.effectiveness_score,
                            behavioral_justification = EXCLUDED.behavioral_justification,
                            competency_scores_json = EXCLUDED.competency_scores_json,
                            updated_at = EXCLUDED.updated_at
                        """,
                roundId,
                reviewer,
                nodeId,
                optionId,
                request.effectivenessScore(),
                request.behavioralJustification().trim(),
                toJson(request.competencyScores()),
                now,
                now
        );
        jdbcTemplate.update(
                """
                        UPDATE answer_key_review_assignments
                        SET status = CASE WHEN status = 'INVITED' THEN 'IN_PROGRESS' ELSE status END
                        WHERE round_id = ? AND user_id = ? AND assignment_role = 'EXPERT'
                        """,
                roundId,
                reviewer
        );
        recordEvent(roundId, "OPTION_REVIEWED", reviewer, Map.of(
                "nodeId", nodeId,
                "optionId", option.getOptionId(),
                "effectivenessScore", request.effectivenessScore()
        ));
        return getSummary(simulationId, versionNumber, roundId);
    }

    @Transactional
    public ReviewSummaryResponse submitExpertReview(String simulationId, int versionNumber, UUID roundId) {
        ReviewRound round = findRound(roundId, simulationId, versionNumber);
        assertMutable(round);
        String reviewer = currentUserService.requiredUserId();
        requireAssignment(roundId, reviewer, AssignmentRole.EXPERT);
        SimulationVersionEntity version = loadVersion(simulationId, versionNumber);
        int expectedOptions = allOptions(version).size();
        Integer reviewedOptions = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM answer_key_option_reviews
                        WHERE round_id = ? AND reviewer_user_id = ?
                        """,
                Integer.class,
                roundId,
                reviewer
        );
        if (reviewedOptions == null || reviewedOptions < expectedOptions) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Revise todas as alternativas antes de concluir. Revisadas: " + reviewedOptions + " de " + expectedOptions + "."
            );
        }
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                        UPDATE answer_key_review_assignments
                        SET status = 'SUBMITTED', submitted_at = ?
                        WHERE round_id = ? AND user_id = ? AND assignment_role = 'EXPERT'
                        """,
                now,
                roundId,
                reviewer
        );
        recordEvent(roundId, "EXPERT_REVIEW_SUBMITTED", reviewer, Map.of("reviewedOptions", reviewedOptions));
        return getSummary(simulationId, versionNumber, roundId);
    }

    @Transactional
    public ReviewSummaryResponse approve(String simulationId, int versionNumber, UUID roundId) {
        ReviewRound round = findRound(roundId, simulationId, versionNumber);
        assertMutable(round);
        String approver = currentUserService.requiredUserId();
        requireAssignment(roundId, approver, AssignmentRole.APPROVER);
        ReviewSummaryResponse summary = getSummary(simulationId, versionNumber, roundId);
        if (!summary.approvable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O gabarito ainda possui bloqueios: " + String.join(" | ", summary.blockers())
            );
        }

        SimulationVersionEntity version = loadVersion(simulationId, versionNumber);
        String fingerprint = calculateFingerprint(version, roundId);
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                        UPDATE answer_key_review_rounds
                        SET status = 'APPROVED', content_fingerprint = ?, approved_by = ?, approved_at = ?
                        WHERE id = ?
                        """,
                fingerprint,
                approver,
                now,
                roundId
        );
        jdbcTemplate.update(
                """
                        UPDATE answer_key_review_assignments
                        SET status = 'APPROVED', submitted_at = COALESCE(submitted_at, ?)
                        WHERE round_id = ? AND user_id = ? AND assignment_role = 'APPROVER'
                        """,
                now,
                roundId,
                approver
        );
        recordEvent(roundId, "ROUND_APPROVED", approver, Map.of("contentFingerprint", fingerprint));
        return getSummary(simulationId, versionNumber, roundId);
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getLatestSummary(String simulationId, int versionNumber) {
        loadVersion(simulationId, versionNumber);
        String empresaId = currentEmpresaService.requiredEmpresaId();
        List<UUID> ids = jdbcTemplate.query(
                """
                        SELECT id
                        FROM answer_key_review_rounds
                        WHERE empresa_id = ? AND simulation_id = ? AND version_number = ?
                        ORDER BY round_number DESC
                        LIMIT 1
                        """,
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class),
                empresaId,
                simulationId,
                versionNumber
        );
        if (ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ainda não existe rodada de revisão para esta versão.");
        }
        return getSummary(simulationId, versionNumber, ids.getFirst());
    }

    @Transactional(readOnly = true)
    public ReviewSummaryResponse getSummary(String simulationId, int versionNumber, UUID roundId) {
        ReviewRound round = findRound(roundId, simulationId, versionNumber);
        SimulationVersionEntity version = loadVersion(simulationId, versionNumber);
        List<SimulationOptionEntity> expected = allOptions(version);
        List<AssignmentResponse> assignments = loadAssignments(roundId);
        List<EvidenceResponse> evidence = loadEvidence(roundId);
        List<ReviewValue> reviews = loadSubmittedReviews(roundId);
        List<String> blockers = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        long submittedExperts = assignments.stream()
                .filter(assignment -> assignment.role() == AssignmentRole.EXPERT)
                .filter(assignment -> "SUBMITTED".equals(assignment.status()))
                .count();
        if (submittedExperts < round.minimumExperts()) {
            blockers.add("São necessários " + round.minimumExperts() + " especialistas concluídos; existem " + submittedExperts + ".");
        }

        Set<String> evidenceNodes = new LinkedHashSet<>();
        evidence.forEach(item -> evidenceNodes.add(item.nodeId()));
        version.getNodes().stream()
                .filter(node -> !node.isFinal())
                .sorted(Comparator.comparing(SimulationNodeEntity::getNodeId))
                .filter(node -> !evidenceNodes.contains(node.getNodeId()))
                .forEach(node -> blockers.add("A etapa " + node.getNodeId() + " não possui tarefa, risco e indicador vinculados."));

        Map<OptionKey, List<ReviewValue>> reviewsByOption = new HashMap<>();
        for (ReviewValue review : reviews) {
            reviewsByOption.computeIfAbsent(new OptionKey(review.nodeId(), review.optionId()), ignored -> new ArrayList<>())
                    .add(review);
        }

        List<OptionConsensusResponse> optionSummaries = new ArrayList<>();
        int reviewedOptions = 0;
        for (SimulationOptionEntity option : expected) {
            String nodeId = option.getSimulationNode().getNodeId();
            List<ReviewValue> optionReviews = reviewsByOption.getOrDefault(new OptionKey(nodeId, option.getOptionId()), List.of());
            if (!optionReviews.isEmpty()) {
                reviewedOptions++;
            }
            BigDecimal average = average(optionReviews);
            BigDecimal dispersion = dispersion(optionReviews, average);
            BigDecimal consensus = consensusFromDispersion(dispersion);
            String status = "READY";
            if (optionReviews.size() < round.minimumExperts()) {
                status = "MISSING_REVIEWS";
                blockers.add("A alternativa " + nodeId + "/" + option.getOptionId() + " não atingiu a quantidade mínima de avaliações.");
            } else if (consensus.compareTo(round.minimumConsensus()) < 0) {
                status = "LOW_CONSENSUS";
                blockers.add("A alternativa " + nodeId + "/" + option.getOptionId() + " possui consenso abaixo do mínimo.");
            } else if (dispersion.compareTo(AMBIGUITY_DISPERSION) >= 0) {
                status = "AMBIGUOUS";
                warnings.add("A alternativa " + nodeId + "/" + option.getOptionId() + " apresenta dispersão relevante e deve ser revisada.");
            }
            optionSummaries.add(new OptionConsensusResponse(
                    nodeId,
                    option.getOptionId(),
                    option.getText(),
                    optionReviews.size(),
                    average,
                    dispersion,
                    consensus,
                    status,
                    optionReviews.stream().map(ReviewValue::justification).distinct().toList()
            ));
        }

        List<ReviewEventResponse> history = loadHistory(roundId);
        boolean approvable = blockers.isEmpty() && !"APPROVED".equals(round.status());
        if ("APPROVED".equals(round.status())) {
            approvable = true;
        }
        return new ReviewSummaryResponse(
                round.toResponse(),
                approvable,
                expected.size(),
                reviewedOptions,
                Math.toIntExact(submittedExperts),
                List.copyOf(blockers),
                List.copyOf(warnings),
                assignments,
                evidence,
                optionSummaries,
                history
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportTechnicalReport(String simulationId, int versionNumber, UUID roundId) {
        ReviewSummaryResponse summary = getSummary(simulationId, versionNumber, roundId);
        StringBuilder csv = new StringBuilder();
        csv.append("secao;etapa;alternativa;competencia;indicador;peso;media;dispersao;consenso;avaliacoes;status;detalhe\n");
        Map<String, List<EvidenceResponse>> evidenceByNode = new LinkedHashMap<>();
        summary.evidence().forEach(item -> evidenceByNode.computeIfAbsent(item.nodeId(), ignored -> new ArrayList<>()).add(item));
        for (OptionConsensusResponse option : summary.options()) {
            List<EvidenceResponse> nodeEvidence = evidenceByNode.getOrDefault(option.nodeId(), List.of());
            if (nodeEvidence.isEmpty()) {
                appendCsv(csv, "MATRIZ", option.nodeId(), option.optionId(), "", "", "", option.averageScore(),
                        option.dispersion(), option.consensus(), option.reviewCount(), option.status(), option.optionText());
            } else {
                for (EvidenceResponse evidence : nodeEvidence) {
                    appendCsv(csv, "MATRIZ", option.nodeId(), option.optionId(), evidence.competency(), evidence.indicator(),
                            evidence.weight(), option.averageScore(), option.dispersion(), option.consensus(),
                            option.reviewCount(), option.status(), evidence.task() + " | Risco: " + evidence.risk());
                }
            }
        }
        for (ReviewEventResponse event : summary.history()) {
            appendCsv(csv, "HISTORICO", "", "", "", event.eventType(), "", "", "", "", "", "",
                    event.occurredAt() + " | " + event.actorUserId() + " | " + event.eventDataJson());
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public void requireApproved(String simulationId, int versionNumber) {
        SimulationVersionEntity version = loadVersion(simulationId, versionNumber);
        if (version.getStatus() == SimulationVersionStatus.PUBLISHED) {
            return;
        }
        String empresaId = currentEmpresaService.requiredEmpresaId();
        List<ApprovedRound> rounds = jdbcTemplate.query(
                """
                        SELECT id, content_fingerprint
                        FROM answer_key_review_rounds
                        WHERE empresa_id = ? AND simulation_id = ? AND version_number = ? AND status = 'APPROVED'
                        ORDER BY round_number DESC
                        LIMIT 1
                        """,
                (resultSet, rowNum) -> new ApprovedRound(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("content_fingerprint")
                ),
                empresaId,
                simulationId,
                versionNumber
        );
        if (rounds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A publicação exige gabarito revisado e aprovado por especialistas do cargo."
            );
        }
        ApprovedRound approved = rounds.getFirst();
        String currentFingerprint = calculateFingerprint(version, approved.id());
        if (!Objects.equals(approved.fingerprint(), currentFingerprint)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "O conteúdo ou a pontuação mudou após a aprovação. Crie uma nova rodada e aprove novamente."
            );
        }
    }

    static BigDecimal consensusFromDispersion(BigDecimal dispersion) {
        BigDecimal normalized = BigDecimal.ONE.subtract(dispersion.divide(new BigDecimal("100"), 8, RoundingMode.HALF_UP));
        return normalized.max(BigDecimal.ZERO).min(BigDecimal.ONE).setScale(4, RoundingMode.HALF_UP);
    }

    private ReviewRound findRound(UUID roundId, String simulationId, int versionNumber) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        List<ReviewRound> rows = jdbcTemplate.query(
                """
                        SELECT id, simulation_id, version_number, round_number, status, minimum_experts,
                               minimum_consensus, created_by, created_at, approved_by, approved_at
                        FROM answer_key_review_rounds
                        WHERE id = ? AND empresa_id = ? AND simulation_id = ? AND version_number = ?
                        """,
                (resultSet, rowNum) -> new ReviewRound(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("simulation_id"),
                        resultSet.getInt("version_number"),
                        resultSet.getInt("round_number"),
                        resultSet.getString("status"),
                        resultSet.getInt("minimum_experts"),
                        resultSet.getBigDecimal("minimum_consensus"),
                        resultSet.getString("created_by"),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getString("approved_by"),
                        resultSet.getTimestamp("approved_at") == null ? null : resultSet.getTimestamp("approved_at").toInstant()
                ),
                roundId,
                empresaId,
                simulationId,
                versionNumber
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rodada de revisão não encontrada.");
        }
        return rows.getFirst();
    }

    private SimulationVersionEntity loadVersion(String simulationId, int versionNumber) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                        empresaId,
                        simulationId,
                        versionNumber
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão da avaliação não encontrada."));
    }

    private void assertDraft(SimulationVersionEntity version) {
        if (version.getStatus() != SimulationVersionStatus.DRAFT) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A revisão do gabarito deve ocorrer em uma versão em rascunho. Clone a versão publicada para editar."
            );
        }
    }

    private void assertMutable(ReviewRound round) {
        if ("APPROVED".equals(round.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A rodada aprovada é imutável. Crie uma nova rodada.");
        }
    }

    private void requireAssignment(UUID roundId, String userId, AssignmentRole role) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM answer_key_review_assignments
                        WHERE round_id = ? AND user_id = ? AND assignment_role = ?
                        """,
                Integer.class,
                roundId,
                userId,
                role.name()
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    role == AssignmentRole.EXPERT
                            ? "Você não foi convidado como especialista desta rodada."
                            : "Você não foi designado como aprovador desta rodada."
            );
        }
    }

    private SimulationNodeEntity findNode(SimulationVersionEntity version, String nodeId) {
        return version.getNodes().stream()
                .filter(node -> node.getNodeId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa não encontrada na versão."));
    }

    private SimulationOptionEntity findOption(SimulationVersionEntity version, String nodeId, String optionId) {
        return findNode(version, nodeId).getOptions().stream()
                .filter(option -> option.getOptionId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alternativa não encontrada na etapa."));
    }

    private void validateCompetencyScores(SimulationVersionEntity version, Map<String, Integer> scores) {
        Set<String> configured = new LinkedHashSet<>();
        version.getCompetencies().forEach(competency -> configured.add(competency.getName().toLowerCase()));
        for (String competency : scores.keySet()) {
            if (!configured.contains(competency.trim().toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Competência não configurada na versão: " + competency);
            }
        }
    }

    private List<SimulationOptionEntity> allOptions(SimulationVersionEntity version) {
        return version.getNodes().stream()
                .sorted(Comparator.comparing(SimulationNodeEntity::getNodeId))
                .flatMap(node -> node.getOptions().stream().sorted(Comparator.comparing(SimulationOptionEntity::getOptionId)))
                .toList();
    }

    private List<AssignmentResponse> loadAssignments(UUID roundId) {
        return jdbcTemplate.query(
                """
                        SELECT user_id, assignment_role, status, invited_at, submitted_at
                        FROM answer_key_review_assignments
                        WHERE round_id = ?
                        ORDER BY assignment_role, user_id
                        """,
                (resultSet, rowNum) -> new AssignmentResponse(
                        resultSet.getString("user_id"),
                        AssignmentRole.valueOf(resultSet.getString("assignment_role")),
                        resultSet.getString("status"),
                        resultSet.getTimestamp("invited_at").toInstant(),
                        resultSet.getTimestamp("submitted_at") == null ? null : resultSet.getTimestamp("submitted_at").toInstant()
                ),
                roundId
        );
    }

    private List<EvidenceResponse> loadEvidence(UUID roundId) {
        return jdbcTemplate.query(
                """
                        SELECT node_id, task_text, risk_text, competency_name, indicator, evidence_weight,
                               updated_by, updated_at
                        FROM answer_key_evidence_links
                        WHERE round_id = ?
                        ORDER BY node_id, competency_name, indicator
                        """,
                (resultSet, rowNum) -> new EvidenceResponse(
                        resultSet.getString("node_id"),
                        resultSet.getString("task_text"),
                        resultSet.getString("risk_text"),
                        resultSet.getString("competency_name"),
                        resultSet.getString("indicator"),
                        resultSet.getBigDecimal("evidence_weight"),
                        resultSet.getString("updated_by"),
                        resultSet.getTimestamp("updated_at").toInstant()
                ),
                roundId
        );
    }

    private List<ReviewValue> loadSubmittedReviews(UUID roundId) {
        return jdbcTemplate.query(
                """
                        SELECT review.node_id, review.option_id, review.effectiveness_score,
                               review.behavioral_justification
                        FROM answer_key_option_reviews review
                        JOIN answer_key_review_assignments assignment
                          ON assignment.round_id = review.round_id
                         AND assignment.user_id = review.reviewer_user_id
                         AND assignment.assignment_role = 'EXPERT'
                         AND assignment.status = 'SUBMITTED'
                        WHERE review.round_id = ?
                        ORDER BY review.node_id, review.option_id, review.reviewer_user_id
                        """,
                (resultSet, rowNum) -> new ReviewValue(
                        resultSet.getString("node_id"),
                        resultSet.getString("option_id"),
                        resultSet.getInt("effectiveness_score"),
                        resultSet.getString("behavioral_justification")
                ),
                roundId
        );
    }

    private List<ReviewEventResponse> loadHistory(UUID roundId) {
        return jdbcTemplate.query(
                """
                        SELECT event_type, actor_user_id, event_data_json, occurred_at
                        FROM answer_key_review_events
                        WHERE round_id = ?
                        ORDER BY occurred_at, id
                        """,
                (resultSet, rowNum) -> new ReviewEventResponse(
                        resultSet.getString("event_type"),
                        resultSet.getString("actor_user_id"),
                        resultSet.getString("event_data_json"),
                        resultSet.getTimestamp("occurred_at").toInstant()
                ),
                roundId
        );
    }

    private BigDecimal average(List<ReviewValue> reviews) {
        if (reviews.isEmpty()) {
            return BigDecimal.ZERO.setScale(4);
        }
        BigDecimal sum = reviews.stream()
                .map(review -> BigDecimal.valueOf(review.score()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(reviews.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal dispersion(List<ReviewValue> reviews, BigDecimal average) {
        if (reviews.size() < 2) {
            return BigDecimal.ZERO.setScale(4);
        }
        double mean = average.doubleValue();
        double variance = reviews.stream()
                .mapToDouble(review -> Math.pow(review.score() - mean, 2))
                .average()
                .orElse(0.0);
        return BigDecimal.valueOf(Math.sqrt(variance)).setScale(4, RoundingMode.HALF_UP);
    }

    private String calculateFingerprint(SimulationVersionEntity version, UUID roundId) {
        StringBuilder canonical = new StringBuilder();
        canonical.append(version.getSimulation().getId()).append('|').append(version.getVersionNumber()).append('\n');
        version.getNodes().stream()
                .sorted(Comparator.comparing(SimulationNodeEntity::getNodeId))
                .forEach(node -> {
                    canonical.append("NODE|").append(node.getNodeId()).append('|')
                            .append(normalize(node.getMessage())).append('|')
                            .append(node.isFinal()).append('|')
                            .append(normalize(node.getReportText())).append('\n');
                    node.getOptions().stream()
                            .sorted(Comparator.comparing(SimulationOptionEntity::getOptionId))
                            .forEach(option -> {
                                canonical.append("OPTION|").append(node.getNodeId()).append('|')
                                        .append(option.getOptionId()).append('|')
                                        .append(normalize(option.getText())).append('|')
                                        .append(normalize(option.getNextNodeId())).append('|')
                                        .append(option.isCritical()).append('|')
                                        .append(normalize(option.getAuditNote())).append('\n');
                                option.getCompetencyScores().stream()
                                        .sorted(Comparator.comparing(OptionCompetencyScoreEntity::getCompetencyName))
                                        .forEach(score -> canonical.append("SCORE|")
                                                .append(score.getCompetencyName()).append('|')
                                                .append(score.getScore()).append('\n'));
                            });
                });
        loadEvidence(roundId).forEach(evidence -> canonical.append("EVIDENCE|")
                .append(evidence.nodeId()).append('|')
                .append(normalize(evidence.task())).append('|')
                .append(normalize(evidence.risk())).append('|')
                .append(normalize(evidence.competency())).append('|')
                .append(normalize(evidence.indicator())).append('|')
                .append(evidence.weight()).append('\n'));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    private void recordEvent(UUID roundId, String eventType, String actor, Map<String, ?> data) {
        jdbcTemplate.update(
                """
                        INSERT INTO answer_key_review_events (
                            round_id, event_type, actor_user_id, event_data_json, occurred_at
                        ) VALUES (?, ?, ?, ?, ?)
                        """,
                roundId,
                eventType,
                actor,
                toJson(data),
                Instant.now()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível serializar os dados da revisão.", exception);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private void appendCsv(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(';');
            }
            String value = values[index] == null ? "" : values[index].toString();
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append('\n');
    }

    private record ReviewRound(
            UUID id,
            String simulationId,
            int versionNumber,
            int roundNumber,
            String status,
            int minimumExperts,
            BigDecimal minimumConsensus,
            String createdBy,
            Instant createdAt,
            String approvedBy,
            Instant approvedAt
    ) {
        RoundResponse toResponse() {
            return new RoundResponse(
                    id,
                    simulationId,
                    versionNumber,
                    roundNumber,
                    status,
                    minimumExperts,
                    minimumConsensus,
                    createdBy,
                    createdAt,
                    approvedBy,
                    approvedAt
            );
        }
    }

    private record ReviewValue(String nodeId, String optionId, int score, String justification) {
    }

    private record OptionKey(String nodeId, String optionId) {
    }

    private record ApprovedRound(UUID id, String fingerprint) {
    }
}
