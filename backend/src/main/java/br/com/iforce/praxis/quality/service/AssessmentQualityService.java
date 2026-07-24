package br.com.iforce.praxis.quality.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.AlternativeMetric;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.AnalyticalRecommendation;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.CategoryMean;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.CompetencyEstimate;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.CriterionType;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.ExternalCriterionRelation;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.ExternalCriterionRequest;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.ExternalCriterionResponse;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.Methodology;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.ObservedSummary;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.PathMetric;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.QualityReportResponse;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.QualityScope;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.ScenarioMetric;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.ScoreBucket;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.SensitiveAnalysis;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.SensitiveAnalysisRequest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AssessmentQualityService {

    private static final int MINIMUM_GENERAL_SAMPLE = 30;
    private static final int MINIMUM_SENSITIVE_GROUP_SAMPLE = 10;
    private static final double Z_95 = 1.96;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;

    public AssessmentQualityService(
            NamedParameterJdbcTemplate jdbcTemplate,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public QualityReportResponse report(
            String simulationId,
            Integer simulationVersionNumber,
            Long gupyJobId,
            Instant from,
            Instant to
    ) {
        QualityContext context = loadContext(simulationId, simulationVersionNumber, gupyJobId, from, to);
        return assembleReport(context, null);
    }

    @Transactional
    public QualityReportResponse sensitiveReport(
            String simulationId,
            Integer simulationVersionNumber,
            Long gupyJobId,
            Instant from,
            Instant to,
            SensitiveAnalysisRequest request
    ) {
        QualityContext context = loadContext(simulationId, simulationVersionNumber, gupyJobId, from, to);
        SensitiveAnalysis sensitiveAnalysis = calculateSensitiveAnalysis(context, request);
        return assembleReport(context, sensitiveAnalysis);
    }

    @Transactional
    public ExternalCriterionResponse saveExternalCriterion(ExternalCriterionRequest request) {
        validateCriterionValue(request);
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actor = currentUserService.requiredUserId();
        Integer attempts = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM candidate_attempts WHERE id = :attemptId AND empresa_id = :empresaId",
                new MapSqlParameterSource()
                        .addValue("attemptId", request.candidateAttemptId())
                        .addValue("empresaId", empresaId),
                Integer.class
        );
        if (attempts == null || attempts == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada na empresa atual.");
        }

        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        Instant observedAt = request.observedAt() == null ? now : request.observedAt();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("empresaId", empresaId)
                .addValue("attemptId", request.candidateAttemptId())
                .addValue("criterionCode", normalizeCode(request.criterionCode()))
                .addValue("criterionLabel", request.criterionLabel().trim())
                .addValue("criterionType", request.criterionType().name())
                .addValue("numericValue", request.numericValue())
                .addValue("categoryValue", normalizeNullable(request.categoryValue()))
                .addValue("observedAt", observedAt)
                .addValue("createdBy", actor)
                .addValue("createdAt", now);
        jdbcTemplate.update("""
                INSERT INTO assessment_external_criteria (
                    id, empresa_id, candidate_attempt_id, criterion_code, criterion_label,
                    criterion_type, numeric_value, category_value, observed_at, created_by, created_at
                ) VALUES (
                    :id, :empresaId, :attemptId, :criterionCode, :criterionLabel,
                    :criterionType, :numericValue, :categoryValue, :observedAt, :createdBy, :createdAt
                )
                ON CONFLICT (empresa_id, candidate_attempt_id, criterion_code)
                DO UPDATE SET
                    criterion_label = EXCLUDED.criterion_label,
                    criterion_type = EXCLUDED.criterion_type,
                    numeric_value = EXCLUDED.numeric_value,
                    category_value = EXCLUDED.category_value,
                    observed_at = EXCLUDED.observed_at,
                    created_by = EXCLUDED.created_by,
                    created_at = EXCLUDED.created_at
                """, parameters);

        return findExternalCriterion(empresaId, request.candidateAttemptId(), normalizeCode(request.criterionCode()));
    }

    @Transactional(readOnly = true)
    public List<ExternalCriterionResponse> listExternalCriteria(String simulationId, Integer versionNumber) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        StringBuilder sql = new StringBuilder("""
                SELECT ec.id, ec.candidate_attempt_id, ec.criterion_code, ec.criterion_label,
                       ec.criterion_type, ec.numeric_value, ec.category_value, ec.observed_at, ec.created_at
                  FROM assessment_external_criteria ec
                  JOIN candidate_attempts ca ON ca.id = ec.candidate_attempt_id
                 WHERE ec.empresa_id = :empresaId
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("empresaId", empresaId);
        if (simulationId != null && !simulationId.isBlank()) {
            sql.append(" AND ca.simulation_id = :simulationId");
            parameters.addValue("simulationId", simulationId.trim());
        }
        if (versionNumber != null) {
            sql.append(" AND ca.simulation_version_number = :versionNumber");
            parameters.addValue("versionNumber", versionNumber);
        }
        sql.append(" ORDER BY ec.observed_at DESC, ec.criterion_code");
        return jdbcTemplate.query(sql.toString(), parameters, (resultSet, rowNumber) -> new ExternalCriterionResponse(
                resultSet.getString("id"),
                resultSet.getString("candidate_attempt_id"),
                resultSet.getString("criterion_code"),
                resultSet.getString("criterion_label"),
                CriterionType.valueOf(resultSet.getString("criterion_type")),
                resultSet.getBigDecimal("numeric_value"),
                resultSet.getString("category_value"),
                resultSet.getTimestamp("observed_at").toInstant(),
                resultSet.getTimestamp("created_at").toInstant()
        ));
    }

    @Transactional(readOnly = true)
    public byte[] exportTechnicalCsv(
            String simulationId,
            Integer simulationVersionNumber,
            Long gupyJobId,
            Instant from,
            Instant to
    ) {
        QualityReportResponse report = report(simulationId, simulationVersionNumber, gupyJobId, from, to);
        StringBuilder csv = new StringBuilder("section;key;sample;value;diagnostic;evidence_type\n");
        appendCsv(csv, "summary", "sample_size", report.observed().sampleSize(),
                report.observed().sampleSize(), "Dado observado", "OBSERVED");
        appendCsv(csv, "summary", "completion_rate", report.observed().sampleSize(),
                report.observed().completionRatePercent(), "Percentual observado", "OBSERVED");
        appendCsv(csv, "summary", "mean_score", report.observed().completed(),
                report.observed().meanScore(), "Estimativa descritiva", "ESTIMATE");
        for (AlternativeMetric metric : report.alternatives()) {
            appendCsv(csv, "alternative", metric.nodeId() + "/" + metric.optionId(), metric.selectedCount(),
                    metric.selectionPercent(), metric.diagnostic(), metric.evidenceType());
        }
        for (PathMetric metric : report.paths()) {
            appendCsv(csv, "path", metric.pathFingerprint(), metric.count(), metric.frequencyPercent(),
                    String.join(" > ", metric.nodeIds()), metric.evidenceType());
        }
        for (CompetencyEstimate metric : report.competencies()) {
            appendCsv(csv, "competency", metric.competency(), metric.sampleSize(), metric.mean(),
                    "IC95% " + metric.confidenceLow95() + " a " + metric.confidenceHigh95(), metric.evidenceType());
        }
        for (ExternalCriterionRelation metric : report.externalCriteria()) {
            appendCsv(csv, "external_criterion", metric.criterionCode(), metric.sampleSize(),
                    metric.pearsonCorrelation(), metric.interpretation(), metric.evidenceType());
        }
        for (AnalyticalRecommendation recommendation : report.recommendations()) {
            appendCsv(csv, "recommendation", recommendation.code(), 0,
                    null, recommendation.title() + ": " + recommendation.detail(), recommendation.evidenceType());
        }
        csv.append("methodology;minimum_general_sample;")
                .append(report.methodology().minimumGeneralSample())
                .append(';').append(report.methodology().minimumGeneralSample())
                .append(";Conclusões são suprimidas abaixo deste limite;METHODOLOGY\n");
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private QualityReportResponse assembleReport(QualityContext context, SensitiveAnalysis sensitiveAnalysis) {
        ObservedSummary observed = observedSummary(context);
        List<ScoreBucket> distribution = scoreDistribution(context.completedScores());
        List<AlternativeMetric> alternatives = alternativeMetrics(context);
        List<PathMetric> paths = pathMetrics(context);
        List<ScenarioMetric> scenarios = scenarioMetrics(context);
        List<CompetencyEstimate> competencies = competencyMetrics(context);
        List<ExternalCriterionRelation> externalCriteria = externalCriterionRelations(context);
        List<String> warnings = warnings(context, sensitiveAnalysis);
        List<AnalyticalRecommendation> recommendations = recommendations(context, alternatives, scenarios);
        Methodology methodology = new Methodology(
                MINIMUM_GENERAL_SAMPLE,
                MINIMUM_SENSITIVE_GROUP_SAMPLE,
                "0 a 100; resultados históricos mantêm a versão do algoritmo de pontuação",
                "Percentil empírico pela posição p × (n - 1), com interpolação pela posição inteira mais próxima",
                "Erro-padrão da média e intervalo de confiança de 95% por competência; não usa alfa isoladamente",
                "Diferença entre a média final de quem escolheu a alternativa e quem não escolheu, na mesma amostra filtrada",
                List.of(
                        "Associação não demonstra causalidade.",
                        "Resultados dependem da comparabilidade dos caminhos e da qualidade do critério externo.",
                        "Amostras pequenas são sinalizadas ou suprimidas.",
                        "Versões de avaliação diferentes nunca são agregadas quando o filtro de versão está informado.",
                        "O painel não recomenda contratação, reprovação ou ranking automático."
                )
        );
        return new QualityReportResponse(
                Instant.now(),
                context.scope(),
                observed,
                distribution,
                alternatives,
                paths,
                scenarios,
                competencies,
                externalCriteria,
                sensitiveAnalysis,
                recommendations,
                warnings,
                methodology
        );
    }

    private QualityContext loadContext(
            String simulationId,
            Integer simulationVersionNumber,
            Long gupyJobId,
            Instant from,
            Instant to
    ) {
        if (simulationId == null || simulationId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a avaliação para preservar a comparabilidade.");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O início do período não pode ser posterior ao fim.");
        }
        String empresaId = currentEmpresaService.requiredEmpresaId();
        QualityScope scope = new QualityScope(simulationId.trim(), simulationVersionNumber, gupyJobId, from, to);
        MapSqlParameterSource parameters = baseParameters(empresaId, scope);
        String filter = filterSql(scope, "ca");
        List<AttemptRow> attempts = jdbcTemplate.query("""
                SELECT ca.id, ca.status, ca.score, ca.started_at, ca.finished_at
                  FROM candidate_attempts ca
                 WHERE ca.empresa_id = :empresaId
                """ + filter + " ORDER BY ca.created_at", parameters, (resultSet, rowNumber) -> new AttemptRow(
                resultSet.getString("id"),
                resultSet.getString("status"),
                resultSet.getObject("score", Integer.class),
                toInstant(resultSet.getTimestamp("started_at")),
                toInstant(resultSet.getTimestamp("finished_at"))
        ));
        List<String> attemptIds = attempts.stream().map(AttemptRow::id).toList();
        if (attemptIds.isEmpty()) {
            return new QualityContext(empresaId, scope, attempts, List.of(), List.of(), List.of(), List.of(), List.of());
        }
        MapSqlParameterSource listParameters = new MapSqlParameterSource().addValue("attemptIds", attemptIds);
        List<AnswerRow> answers = jdbcTemplate.query("""
                SELECT aa.candidate_attempt_id, aa.node_id, aa.option_id, aa.timed_out,
                       aa.answered_at, ans.served_at, ca.score
                  FROM attempt_answers aa
                  JOIN candidate_attempts ca ON ca.id = aa.candidate_attempt_id
                  LEFT JOIN attempt_node_serves ans
                    ON ans.candidate_attempt_id = aa.candidate_attempt_id AND ans.node_id = aa.node_id
                 WHERE aa.candidate_attempt_id IN (:attemptIds)
                """, listParameters, (resultSet, rowNumber) -> new AnswerRow(
                resultSet.getString("candidate_attempt_id"),
                resultSet.getString("node_id"),
                resultSet.getString("option_id"),
                resultSet.getBoolean("timed_out"),
                toInstant(resultSet.getTimestamp("answered_at")),
                toInstant(resultSet.getTimestamp("served_at")),
                resultSet.getObject("score", Integer.class)
        ));
        List<ServeRow> serves = jdbcTemplate.query("""
                SELECT candidate_attempt_id, node_id, served_at
                  FROM attempt_node_serves
                 WHERE candidate_attempt_id IN (:attemptIds)
                 ORDER BY candidate_attempt_id, served_at, id
                """, listParameters, (resultSet, rowNumber) -> new ServeRow(
                resultSet.getString("candidate_attempt_id"),
                resultSet.getString("node_id"),
                resultSet.getTimestamp("served_at").toInstant()
        ));
        List<CompetencyRow> competencyRows = jdbcTemplate.query("""
                SELECT candidate_attempt_id, name, score
                  FROM result_items
                 WHERE candidate_attempt_id IN (:attemptIds)
                """, listParameters, (resultSet, rowNumber) -> new CompetencyRow(
                resultSet.getString("candidate_attempt_id"),
                resultSet.getString("name"),
                resultSet.getInt("score")
        ));
        List<IntegrityRow> integrityRows = jdbcTemplate.query("""
                SELECT candidate_attempt_id, event_type, detail
                  FROM candidate_integrity_events
                 WHERE candidate_attempt_id IN (:attemptIds)
                   AND event_type IN ('SESSION_RESUMED', 'VIDEO_PLAYBACK_PAUSED')
                """, listParameters, (resultSet, rowNumber) -> new IntegrityRow(
                resultSet.getString("candidate_attempt_id"),
                resultSet.getString("event_type"),
                resultSet.getString("detail")
        ));
        List<CriterionRow> criteria = jdbcTemplate.query("""
                SELECT candidate_attempt_id, criterion_code, criterion_label, criterion_type,
                       numeric_value, category_value
                  FROM assessment_external_criteria
                 WHERE empresa_id = :empresaId
                   AND candidate_attempt_id IN (:attemptIds)
                """, new MapSqlParameterSource()
                .addValue("empresaId", empresaId)
                .addValue("attemptIds", attemptIds), (resultSet, rowNumber) -> new CriterionRow(
                resultSet.getString("candidate_attempt_id"),
                resultSet.getString("criterion_code"),
                resultSet.getString("criterion_label"),
                CriterionType.valueOf(resultSet.getString("criterion_type")),
                resultSet.getBigDecimal("numeric_value"),
                resultSet.getString("category_value")
        ));
        return new QualityContext(empresaId, scope, attempts, answers, serves, competencyRows, integrityRows, criteria);
    }

    private ObservedSummary observedSummary(QualityContext context) {
        long completed = context.attempts().stream().filter(AttemptRow::completed).count();
        long abandoned = context.attempts().stream().filter(AttemptRow::abandonedOrExpired).count();
        List<Integer> scores = context.completedScores();
        Double mean = mean(scores);
        Double standardDeviation = standardDeviation(scores);
        List<Long> durations = context.attempts().stream()
                .filter(attempt -> attempt.startedAt() != null && attempt.finishedAt() != null)
                .map(attempt -> Math.max(0L, Duration.between(attempt.startedAt(), attempt.finishedAt()).getSeconds()))
                .toList();
        long pauseEvents = context.integrityRows().stream()
                .filter(row -> "SESSION_RESUMED".equals(row.eventType()) || "VIDEO_PLAYBACK_PAUSED".equals(row.eventType()))
                .count();
        return new ObservedSummary(
                context.attempts().size(),
                completed,
                abandoned,
                percentage(completed, context.attempts().size()),
                mean,
                standardDeviation,
                percentile(scores, 0.10),
                percentile(scores, 0.25),
                percentile(scores, 0.50),
                percentile(scores, 0.75),
                percentile(scores, 0.90),
                meanLong(durations),
                pauseEvents
        );
    }

    private List<ScoreBucket> scoreDistribution(List<Integer> scores) {
        List<ScoreBucket> buckets = new ArrayList<>();
        for (int start = 0; start <= 100; start += 10) {
            int end = start == 100 ? 100 : start + 9;
            int bucketStart = start;
            int bucketEnd = end;
            long count = scores.stream().filter(score -> score >= bucketStart && score <= bucketEnd).count();
            buckets.add(new ScoreBucket(bucketStart, bucketEnd, count, percentage(count, scores.size())));
        }
        return buckets;
    }

    private List<AlternativeMetric> alternativeMetrics(QualityContext context) {
        long answered = context.answers().stream().filter(answer -> !answer.timedOut() && answer.optionId() != null).count();
        Map<String, List<AnswerRow>> grouped = context.answers().stream()
                .filter(answer -> !answer.timedOut() && answer.optionId() != null)
                .collect(Collectors.groupingBy(
                        answer -> answer.nodeId() + "\u0000" + answer.optionId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        Map<String, Integer> scoreByAttempt = context.attempts().stream()
                .filter(attempt -> attempt.score() != null)
                .collect(Collectors.toMap(AttemptRow::id, AttemptRow::score, (first, second) -> first));
        List<AlternativeMetric> metrics = new ArrayList<>();
        for (Map.Entry<String, List<AnswerRow>> entry : grouped.entrySet()) {
            String[] key = entry.getKey().split("\u0000", 2);
            Set<String> selectedAttempts = entry.getValue().stream().map(AnswerRow::attemptId).collect(Collectors.toSet());
            List<Integer> selectedScores = selectedAttempts.stream().map(scoreByAttempt::get).filter(Objects::nonNull).toList();
            List<Integer> otherScores = scoreByAttempt.entrySet().stream()
                    .filter(score -> !selectedAttempts.contains(score.getKey()))
                    .map(Map.Entry::getValue)
                    .toList();
            Double selectedMean = mean(selectedScores);
            Double difference = selectedMean == null || otherScores.isEmpty()
                    ? null
                    : round(selectedMean - Objects.requireNonNull(mean(otherScores)), 2);
            double selectionPercent = percentage(entry.getValue().size(), answered);
            String diagnostic = alternativeDiagnostic(selectionPercent, difference, context.attempts().size());
            metrics.add(new AlternativeMetric(
                    key[0], key[1], entry.getValue().size(), selectionPercent,
                    selectedMean, difference, diagnostic, "OBSERVED_AND_ESTIMATE"
            ));
        }
        return metrics.stream()
                .sorted(Comparator.comparing(AlternativeMetric::nodeId).thenComparing(AlternativeMetric::optionId))
                .toList();
    }

    private List<PathMetric> pathMetrics(QualityContext context) {
        Map<String, List<ServeRow>> byAttempt = context.serves().stream()
                .collect(Collectors.groupingBy(ServeRow::attemptId, LinkedHashMap::new, Collectors.toList()));
        Map<String, AttemptRow> attemptsById = context.attempts().stream()
                .collect(Collectors.toMap(AttemptRow::id, Function.identity()));
        Map<String, PathAccumulator> paths = new LinkedHashMap<>();
        for (Map.Entry<String, List<ServeRow>> entry : byAttempt.entrySet()) {
            List<String> nodeIds = entry.getValue().stream().map(ServeRow::nodeId).toList();
            String fingerprint = sha256(String.join("\u001F", nodeIds));
            PathAccumulator accumulator = paths.computeIfAbsent(fingerprint, ignored -> new PathAccumulator(nodeIds));
            accumulator.count++;
            AttemptRow attempt = attemptsById.get(entry.getKey());
            if (attempt != null && attempt.score() != null) {
                accumulator.scores.add(attempt.score());
            }
            if (attempt != null && attempt.startedAt() != null && attempt.finishedAt() != null) {
                accumulator.durations.add(Math.max(0L, Duration.between(attempt.startedAt(), attempt.finishedAt()).getSeconds()));
            }
        }
        return paths.entrySet().stream()
                .map(entry -> new PathMetric(
                        entry.getKey(),
                        entry.getValue().nodeIds,
                        entry.getValue().count,
                        percentage(entry.getValue().count, context.attempts().size()),
                        mean(entry.getValue().scores),
                        meanLong(entry.getValue().durations),
                        "OBSERVED"
                ))
                .sorted(Comparator.comparingLong(PathMetric::count).reversed())
                .toList();
    }

    private List<ScenarioMetric> scenarioMetrics(QualityContext context) {
        Map<String, Long> presentations = context.serves().stream()
                .collect(Collectors.groupingBy(ServeRow::nodeId, LinkedHashMap::new, Collectors.counting()));
        Map<String, List<AnswerRow>> answers = context.answers().stream()
                .collect(Collectors.groupingBy(AnswerRow::nodeId, LinkedHashMap::new, Collectors.toList()));
        Set<String> nodeIds = new LinkedHashSet<>();
        nodeIds.addAll(presentations.keySet());
        nodeIds.addAll(answers.keySet());
        List<ScenarioMetric> metrics = new ArrayList<>();
        for (String nodeId : nodeIds) {
            List<AnswerRow> nodeAnswers = answers.getOrDefault(nodeId, List.of());
            List<Long> responseSeconds = nodeAnswers.stream()
                    .filter(answer -> answer.servedAt() != null && answer.answeredAt() != null)
                    .map(answer -> Math.max(0L, Duration.between(answer.servedAt(), answer.answeredAt()).getSeconds()))
                    .toList();
            long pauses = context.integrityRows().stream()
                    .filter(row -> "VIDEO_PLAYBACK_PAUSED".equals(row.eventType()))
                    .filter(row -> row.detail() != null && row.detail().contains(nodeId))
                    .count();
            metrics.add(new ScenarioMetric(
                    nodeId,
                    presentations.getOrDefault(nodeId, 0L),
                    nodeAnswers.size(),
                    nodeAnswers.stream().filter(AnswerRow::timedOut).count(),
                    meanLong(responseSeconds),
                    pauses,
                    "OBSERVED"
            ));
        }
        return metrics.stream().sorted(Comparator.comparing(ScenarioMetric::nodeId)).toList();
    }

    private List<CompetencyEstimate> competencyMetrics(QualityContext context) {
        Map<String, List<Integer>> grouped = context.competencyRows().stream()
                .collect(Collectors.groupingBy(CompetencyRow::name, LinkedHashMap::new,
                        Collectors.mapping(CompetencyRow::score, Collectors.toList())));
        return grouped.entrySet().stream().map(entry -> {
            List<Integer> values = entry.getValue();
            Double mean = mean(values);
            Double standardDeviation = standardDeviation(values);
            Double standardError = standardDeviation == null || values.isEmpty()
                    ? null
                    : round(standardDeviation / Math.sqrt(values.size()), 3);
            Double margin = standardError == null ? null : Z_95 * standardError;
            return new CompetencyEstimate(
                    entry.getKey(),
                    values.size(),
                    mean,
                    standardDeviation,
                    standardError,
                    mean == null || margin == null ? null : round(Math.max(0, mean - margin), 2),
                    mean == null || margin == null ? null : round(Math.min(100, mean + margin), 2),
                    precisionLevel(values.size(), standardError),
                    "ESTIMATE"
            );
        }).sorted(Comparator.comparing(CompetencyEstimate::competency)).toList();
    }

    private List<ExternalCriterionRelation> externalCriterionRelations(QualityContext context) {
        Map<String, Integer> scoreByAttempt = context.attempts().stream()
                .filter(attempt -> attempt.score() != null)
                .collect(Collectors.toMap(AttemptRow::id, AttemptRow::score, (first, second) -> first));
        Map<String, List<CriterionRow>> grouped = context.criteria().stream()
                .collect(Collectors.groupingBy(CriterionRow::code, LinkedHashMap::new, Collectors.toList()));
        List<ExternalCriterionRelation> relations = new ArrayList<>();
        for (Map.Entry<String, List<CriterionRow>> entry : grouped.entrySet()) {
            List<CriterionRow> rows = entry.getValue();
            CriterionRow first = rows.getFirst();
            if (first.type() == CriterionType.NUMERIC) {
                List<NumericPair> pairs = rows.stream()
                        .filter(row -> row.numericValue() != null && scoreByAttempt.containsKey(row.attemptId()))
                        .map(row -> new NumericPair(scoreByAttempt.get(row.attemptId()), row.numericValue().doubleValue()))
                        .toList();
                Double correlation = pairs.size() < MINIMUM_GENERAL_SAMPLE ? null : pearson(pairs);
                relations.add(new ExternalCriterionRelation(
                        entry.getKey(), first.label(), first.type(), pairs.size(), correlation, List.of(),
                        correlation == null
                                ? "Amostra insuficiente para estimar associação com segurança."
                                : correlationInterpretation(correlation),
                        "ESTIMATE"
                ));
            } else {
                Map<String, List<Integer>> categories = rows.stream()
                        .filter(row -> row.categoryValue() != null && scoreByAttempt.containsKey(row.attemptId()))
                        .collect(Collectors.groupingBy(CriterionRow::categoryValue, LinkedHashMap::new,
                                Collectors.mapping(row -> scoreByAttempt.get(row.attemptId()), Collectors.toList())));
                List<CategoryMean> categoryMeans = categoryMeans(categories, MINIMUM_SENSITIVE_GROUP_SAMPLE);
                long sample = categories.values().stream().mapToLong(List::size).sum();
                relations.add(new ExternalCriterionRelation(
                        entry.getKey(), first.label(), first.type(), sample, null, categoryMeans,
                        sample < MINIMUM_GENERAL_SAMPLE
                                ? "Amostra insuficiente para comparar categorias além da descrição agregada."
                                : "Diferenças são descritivas e precisam de análise contextual; não demonstram causalidade.",
                        "OBSERVED_AND_ESTIMATE"
                ));
            }
        }
        return relations;
    }

    private SensitiveAnalysis calculateSensitiveAnalysis(QualityContext context, SensitiveAnalysisRequest request) {
        int minimumSample = request.minimumSample() == null
                ? MINIMUM_SENSITIVE_GROUP_SAMPLE
                : Math.max(MINIMUM_SENSITIVE_GROUP_SAMPLE, request.minimumSample());
        String code = normalizeCode(request.groupCriterionCode());
        Map<String, Integer> scoreByAttempt = context.attempts().stream()
                .filter(attempt -> attempt.score() != null)
                .collect(Collectors.toMap(AttemptRow::id, AttemptRow::score, (first, second) -> first));
        Map<String, List<Integer>> grouped = context.criteria().stream()
                .filter(row -> row.code().equals(code))
                .filter(row -> row.type() == CriterionType.CATEGORY)
                .filter(row -> row.categoryValue() != null && scoreByAttempt.containsKey(row.attemptId()))
                .collect(Collectors.groupingBy(CriterionRow::categoryValue, LinkedHashMap::new,
                        Collectors.mapping(row -> scoreByAttempt.get(row.attemptId()), Collectors.toList())));
        if (grouped.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "O critério de grupo não possui valores categóricos na amostra filtrada.");
        }
        List<CategoryMean> groups = categoryMeans(grouped, minimumSample);
        int suppressed = (int) groups.stream().filter(CategoryMean::suppressed).count();
        int visible = groups.size() - suppressed;
        UUID auditId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO assessment_quality_sensitive_audits (
                    id, empresa_id, simulation_id, simulation_version_number, gupy_job_id,
                    group_criterion_code, purpose, legal_basis, minimum_sample,
                    visible_groups, suppressed_groups, requested_by, requested_at
                ) VALUES (
                    :id, :empresaId, :simulationId, :versionNumber, :gupyJobId,
                    :groupCode, :purpose, :legalBasis, :minimumSample,
                    :visibleGroups, :suppressedGroups, :requestedBy, :requestedAt
                )
                """, new MapSqlParameterSource()
                .addValue("id", auditId)
                .addValue("empresaId", context.empresaId())
                .addValue("simulationId", context.scope().simulationId())
                .addValue("versionNumber", context.scope().simulationVersionNumber())
                .addValue("gupyJobId", context.scope().gupyJobId())
                .addValue("groupCode", code)
                .addValue("purpose", request.purpose().trim())
                .addValue("legalBasis", request.legalBasis().trim())
                .addValue("minimumSample", minimumSample)
                .addValue("visibleGroups", visible)
                .addValue("suppressedGroups", suppressed)
                .addValue("requestedBy", currentUserService.requiredUserId())
                .addValue("requestedAt", Instant.now()));
        return new SensitiveAnalysis(
                code,
                minimumSample,
                groups,
                suppressed,
                request.purpose().trim(),
                request.legalBasis().trim(),
                auditId.toString()
        );
    }

    private List<CategoryMean> categoryMeans(Map<String, List<Integer>> grouped, int minimumSample) {
        return grouped.entrySet().stream()
                .map(entry -> {
                    boolean suppressed = entry.getValue().size() < minimumSample;
                    return new CategoryMean(
                            entry.getKey(),
                            entry.getValue().size(),
                            suppressed ? null : mean(entry.getValue()),
                            suppressed
                    );
                })
                .sorted(Comparator.comparing(CategoryMean::category))
                .toList();
    }

    private List<String> warnings(QualityContext context, SensitiveAnalysis sensitiveAnalysis) {
        List<String> warnings = new ArrayList<>();
        if (context.attempts().size() < MINIMUM_GENERAL_SAMPLE) {
            warnings.add("A amostra possui menos de " + MINIMUM_GENERAL_SAMPLE
                    + " tentativas. Percentis, discriminação e relações externas são apenas exploratórios.");
        }
        if (context.scope().simulationVersionNumber() == null) {
            warnings.add("Nenhuma versão foi filtrada. Versões diferentes podem representar conteúdos incompatíveis; filtre uma versão antes de concluir.");
        }
        if (context.completedScores().isEmpty()) {
            warnings.add("Não há tentativas concluídas com nota no período selecionado.");
        }
        if (sensitiveAnalysis != null && sensitiveAnalysis.suppressedGroups() > 0) {
            warnings.add("Um ou mais grupos foram suprimidos por não atingir a amostra mínima definida.");
        }
        return warnings;
    }

    private List<AnalyticalRecommendation> recommendations(
            QualityContext context,
            List<AlternativeMetric> alternatives,
            List<ScenarioMetric> scenarios
    ) {
        List<AnalyticalRecommendation> recommendations = new ArrayList<>();
        if (context.attempts().size() < MINIMUM_GENERAL_SAMPLE) {
            recommendations.add(new AnalyticalRecommendation(
                    "COLLECT_MORE_DATA", "WARNING", "Aumentar a amostra",
                    "Colete pelo menos " + MINIMUM_GENERAL_SAMPLE + " casos comparáveis antes de tomar decisões metodológicas.",
                    "RECOMMENDATION"
            ));
        }
        alternatives.stream()
                .filter(metric -> !"Sem alerta na amostra atual".equals(metric.diagnostic()))
                .forEach(metric -> recommendations.add(new AnalyticalRecommendation(
                        "REVIEW_ALTERNATIVE", "WARNING", "Revisar alternativa " + metric.optionId(),
                        metric.diagnostic() + " no cenário " + metric.nodeId() + ".",
                        "RECOMMENDATION"
                )));
        scenarios.stream()
                .filter(metric -> metric.presentations() >= MINIMUM_GENERAL_SAMPLE)
                .filter(metric -> percentage(metric.timeouts(), metric.presentations()) >= 30.0)
                .forEach(metric -> recommendations.add(new AnalyticalRecommendation(
                        "REVIEW_SCENARIO_TIME", "WARNING", "Revisar tempo do cenário " + metric.nodeId(),
                        "O cenário apresenta pelo menos 30% de respostas por tempo esgotado na amostra observada.",
                        "RECOMMENDATION"
                )));
        return recommendations;
    }

    private ExternalCriterionResponse findExternalCriterion(String empresaId, String attemptId, String criterionCode) {
        return jdbcTemplate.queryForObject("""
                SELECT id, candidate_attempt_id, criterion_code, criterion_label, criterion_type,
                       numeric_value, category_value, observed_at, created_at
                  FROM assessment_external_criteria
                 WHERE empresa_id = :empresaId
                   AND candidate_attempt_id = :attemptId
                   AND criterion_code = :criterionCode
                """, new MapSqlParameterSource()
                .addValue("empresaId", empresaId)
                .addValue("attemptId", attemptId)
                .addValue("criterionCode", criterionCode), (resultSet, rowNumber) -> new ExternalCriterionResponse(
                resultSet.getString("id"),
                resultSet.getString("candidate_attempt_id"),
                resultSet.getString("criterion_code"),
                resultSet.getString("criterion_label"),
                CriterionType.valueOf(resultSet.getString("criterion_type")),
                resultSet.getBigDecimal("numeric_value"),
                resultSet.getString("category_value"),
                resultSet.getTimestamp("observed_at").toInstant(),
                resultSet.getTimestamp("created_at").toInstant()
        ));
    }

    private void validateCriterionValue(ExternalCriterionRequest request) {
        if (request.criterionType() == CriterionType.NUMERIC
                && (request.numericValue() == null || normalizeNullable(request.categoryValue()) != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Critério numérico exige numericValue e não aceita categoryValue.");
        }
        if (request.criterionType() == CriterionType.CATEGORY
                && (request.numericValue() != null || normalizeNullable(request.categoryValue()) == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Critério categórico exige categoryValue e não aceita numericValue.");
        }
    }

    private MapSqlParameterSource baseParameters(String empresaId, QualityScope scope) {
        return new MapSqlParameterSource()
                .addValue("empresaId", empresaId)
                .addValue("simulationId", scope.simulationId())
                .addValue("versionNumber", scope.simulationVersionNumber())
                .addValue("gupyJobId", scope.gupyJobId())
                .addValue("from", scope.from())
                .addValue("to", scope.to());
    }

    private String filterSql(QualityScope scope, String alias) {
        StringBuilder filter = new StringBuilder(" AND ").append(alias).append(".simulation_id = :simulationId");
        if (scope.simulationVersionNumber() != null) {
            filter.append(" AND ").append(alias).append(".simulation_version_number = :versionNumber");
        }
        if (scope.gupyJobId() != null) {
            filter.append(" AND ").append(alias).append(".gupy_job_id = :gupyJobId");
        }
        if (scope.from() != null) {
            filter.append(" AND ").append(alias).append(".created_at >= :from");
        }
        if (scope.to() != null) {
            filter.append(" AND ").append(alias).append(".created_at <= :to");
        }
        return filter.toString();
    }

    private String alternativeDiagnostic(double selectionPercent, Double difference, int sampleSize) {
        if (sampleSize < MINIMUM_GENERAL_SAMPLE) {
            return "Amostra insuficiente para classificar poder de diferenciação";
        }
        if (selectionPercent < 5.0) {
            return "Alternativa quase nunca escolhida";
        }
        if (selectionPercent > 80.0) {
            return "Alternativa dominante";
        }
        if (difference != null && Math.abs(difference) < 5.0) {
            return "Baixo poder de diferenciação na nota final";
        }
        return "Sem alerta na amostra atual";
    }

    private String precisionLevel(int sampleSize, Double standardError) {
        if (sampleSize < MINIMUM_GENERAL_SAMPLE) {
            return "INSUFFICIENT_SAMPLE";
        }
        if (standardError == null) {
            return "UNAVAILABLE";
        }
        if (standardError <= 2.5) {
            return "HIGH";
        }
        if (standardError <= 5.0) {
            return "MODERATE";
        }
        return "LOW";
    }

    private String correlationInterpretation(double correlation) {
        double absolute = Math.abs(correlation);
        String strength;
        if (absolute < 0.20) {
            strength = "muito fraca";
        } else if (absolute < 0.40) {
            strength = "fraca";
        } else if (absolute < 0.60) {
            strength = "moderada";
        } else if (absolute < 0.80) {
            strength = "forte";
        } else {
            strength = "muito forte";
        }
        return "Associação " + strength + " e " + (correlation >= 0 ? "positiva" : "negativa")
                + "; não implica causalidade nem decisão automática.";
    }

    private Double pearson(List<NumericPair> pairs) {
        double meanX = pairs.stream().mapToDouble(NumericPair::score).average().orElse(0);
        double meanY = pairs.stream().mapToDouble(NumericPair::criterion).average().orElse(0);
        double numerator = 0;
        double denominatorX = 0;
        double denominatorY = 0;
        for (NumericPair pair : pairs) {
            double dx = pair.score() - meanX;
            double dy = pair.criterion() - meanY;
            numerator += dx * dy;
            denominatorX += dx * dx;
            denominatorY += dy * dy;
        }
        double denominator = Math.sqrt(denominatorX * denominatorY);
        return denominator == 0 ? null : round(numerator / denominator, 4);
    }

    private Double mean(List<Integer> values) {
        if (values.isEmpty()) {
            return null;
        }
        return round(values.stream().mapToInt(Integer::intValue).average().orElse(0), 2);
    }

    private Double meanLong(List<Long> values) {
        if (values.isEmpty()) {
            return null;
        }
        return round(values.stream().mapToLong(Long::longValue).average().orElse(0), 2);
    }

    private Double standardDeviation(List<Integer> values) {
        if (values.size() < 2) {
            return null;
        }
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .sum() / (values.size() - 1);
        return round(Math.sqrt(variance), 2);
    }

    private Integer percentile(List<Integer> values, double percentile) {
        if (values.isEmpty()) {
            return null;
        }
        List<Integer> sorted = values.stream().sorted().toList();
        int index = (int) Math.round(percentile * (sorted.size() - 1));
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0;
        }
        return round(numerator * 100.0 / denominator, 1);
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : hash) {
                result.append(String.format(Locale.ROOT, "%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_.-]", "_");
    }

    private String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Instant toInstant(java.sql.Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private void appendCsv(
            StringBuilder csv,
            String section,
            String key,
            long sample,
            Object value,
            String diagnostic,
            String evidenceType
    ) {
        csv.append(csvValue(section)).append(';')
                .append(csvValue(key)).append(';')
                .append(sample).append(';')
                .append(csvValue(value == null ? "" : value.toString())).append(';')
                .append(csvValue(diagnostic)).append(';')
                .append(csvValue(evidenceType)).append('\n');
    }

    private String csvValue(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private record AttemptRow(
            String id,
            String status,
            Integer score,
            Instant startedAt,
            Instant finishedAt
    ) {
        boolean completed() {
            return "COMPLETED".equals(status);
        }

        boolean abandonedOrExpired() {
            return "ABANDONED".equals(status) || "EXPIRED".equals(status);
        }
    }

    private record AnswerRow(
            String attemptId,
            String nodeId,
            String optionId,
            boolean timedOut,
            Instant answeredAt,
            Instant servedAt,
            Integer finalScore
    ) {
    }

    private record ServeRow(String attemptId, String nodeId, Instant servedAt) {
    }

    private record CompetencyRow(String attemptId, String name, int score) {
    }

    private record IntegrityRow(String attemptId, String eventType, String detail) {
    }

    private record CriterionRow(
            String attemptId,
            String code,
            String label,
            CriterionType type,
            BigDecimal numericValue,
            String categoryValue
    ) {
    }

    private record NumericPair(double score, double criterion) {
    }

    private record QualityContext(
            String empresaId,
            QualityScope scope,
            List<AttemptRow> attempts,
            List<AnswerRow> answers,
            List<ServeRow> serves,
            List<CompetencyRow> competencyRows,
            List<IntegrityRow> integrityRows,
            List<CriterionRow> criteria
    ) {
        List<Integer> completedScores() {
            return attempts.stream()
                    .filter(AttemptRow::completed)
                    .map(AttemptRow::score)
                    .filter(Objects::nonNull)
                    .toList();
        }
    }

    private static final class PathAccumulator {
        private final List<String> nodeIds;
        private final List<Integer> scores = new ArrayList<>();
        private final List<Long> durations = new ArrayList<>();
        private long count;

        private PathAccumulator(List<String> nodeIds) {
            this.nodeIds = List.copyOf(nodeIds);
        }
    }
}
