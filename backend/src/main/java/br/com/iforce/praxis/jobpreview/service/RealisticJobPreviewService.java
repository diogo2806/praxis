package br.com.iforce.praxis.jobpreview.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.candidate.service.CandidateAttemptTokenResolver;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.CandidatePreviewReactionRequest;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.CandidatePreviewResponse;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.CreatePreviewRequest;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.DisplayPosition;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.DisplayStage;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.MediaItem;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.PreviewContentRequest;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.PreviewMetricsResponse;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.PreviewSummaryResponse;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.PreviewVersionResponse;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.ScopeType;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.UpdatePreviewDraftRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RealisticJobPreviewService {

    private static final int MINIMUM_METRICS_SAMPLE = 10;
    private static final String INFORMATIONAL_NOTICE =
            "Esta prévia é informativa, não altera a nota e não substitui descrição de vaga, entrevista ou contrato.";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final CandidateAttemptTokenResolver candidateAttemptTokenResolver;

    public RealisticJobPreviewService(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            CandidateAttemptTokenResolver candidateAttemptTokenResolver
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.candidateAttemptTokenResolver = candidateAttemptTokenResolver;
    }

    @Transactional
    public PreviewVersionResponse create(CreatePreviewRequest request) {
        validateBalancedContent(request.content());
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actor = currentUserService.requiredUserId();
        UUID previewId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        Instant now = Instant.now();
        MapSqlParameterSource previewParameters = new MapSqlParameterSource()
                .addValue("id", previewId)
                .addValue("empresaId", empresaId)
                .addValue("scopeType", request.scopeType().name())
                .addValue("scopeKey", request.scopeKey().trim())
                .addValue("title", request.title().trim())
                .addValue("displayPosition", request.displayPosition().name())
                .addValue("acknowledgementRequired", request.acknowledgementRequired())
                .addValue("actor", actor)
                .addValue("now", now);
        try {
            jdbcTemplate.update("""
                    INSERT INTO realistic_job_previews (
                        id, empresa_id, scope_type, scope_key, title, display_position,
                        acknowledgement_required, created_by, created_at, updated_by, updated_at
                    ) VALUES (
                        :id, :empresaId, :scopeType, :scopeKey, :title, :displayPosition,
                        :acknowledgementRequired, :actor, :now, :actor, :now
                    )
                    """, previewParameters);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Já existe uma prévia para este tipo e identificador de escopo.");
        }
        insertVersion(versionId, previewId, 1, "DRAFT", request.content(), actor, now);
        replaceScenarioLinks(versionId, request.content().scenarioNodeIds());
        return getVersion(previewId.toString(), 1, empresaId);
    }

    @Transactional(readOnly = true)
    public List<PreviewSummaryResponse> list() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return jdbcTemplate.query("""
                SELECT p.id, p.scope_type, p.scope_key, p.title, p.display_position,
                       p.acknowledgement_required, p.active_version_number, p.updated_at,
                       MAX(CASE WHEN v.status = 'DRAFT' THEN v.version_number END) AS draft_version_number
                  FROM realistic_job_previews p
                  LEFT JOIN realistic_job_preview_versions v ON v.preview_id = p.id
                 WHERE p.empresa_id = :empresaId
                 GROUP BY p.id, p.scope_type, p.scope_key, p.title, p.display_position,
                          p.acknowledgement_required, p.active_version_number, p.updated_at
                 ORDER BY p.updated_at DESC
                """, new MapSqlParameterSource("empresaId", empresaId), (resultSet, rowNumber) ->
                new PreviewSummaryResponse(
                        resultSet.getString("id"),
                        ScopeType.valueOf(resultSet.getString("scope_type")),
                        resultSet.getString("scope_key"),
                        resultSet.getString("title"),
                        DisplayPosition.valueOf(resultSet.getString("display_position")),
                        resultSet.getBoolean("acknowledgement_required"),
                        resultSet.getObject("active_version_number", Integer.class),
                        resultSet.getObject("draft_version_number", Integer.class),
                        resultSet.getTimestamp("updated_at").toInstant()
                ));
    }

    @Transactional(readOnly = true)
    public PreviewVersionResponse getDraft(String previewId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        Integer draftVersion = jdbcTemplate.queryForObject("""
                SELECT MAX(v.version_number)
                  FROM realistic_job_preview_versions v
                  JOIN realistic_job_previews p ON p.id = v.preview_id
                 WHERE p.id = CAST(:previewId AS UUID)
                   AND p.empresa_id = :empresaId
                   AND v.status = 'DRAFT'
                """, new MapSqlParameterSource()
                .addValue("previewId", previewId)
                .addValue("empresaId", empresaId), Integer.class);
        if (draftVersion == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rascunho não encontrado.");
        }
        return getVersion(previewId, draftVersion, empresaId);
    }

    @Transactional
    public PreviewVersionResponse updateDraft(String previewId, UpdatePreviewDraftRequest request) {
        validateBalancedContent(request.content());
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actor = currentUserService.requiredUserId();
        PreviewVersionResponse draft = getDraft(previewId);
        jdbcTemplate.update("""
                UPDATE realistic_job_previews
                   SET title = :title,
                       display_position = :displayPosition,
                       acknowledgement_required = :acknowledgementRequired,
                       updated_by = :actor,
                       updated_at = :now
                 WHERE id = CAST(:previewId AS UUID)
                   AND empresa_id = :empresaId
                """, new MapSqlParameterSource()
                .addValue("title", request.title().trim())
                .addValue("displayPosition", request.displayPosition().name())
                .addValue("acknowledgementRequired", request.acknowledgementRequired())
                .addValue("actor", actor)
                .addValue("now", Instant.now())
                .addValue("previewId", previewId)
                .addValue("empresaId", empresaId));
        updateVersion(draft.versionId(), request.content());
        replaceScenarioLinks(UUID.fromString(draft.versionId()), request.content().scenarioNodeIds());
        return getVersion(previewId, draft.versionNumber(), empresaId);
    }

    @Transactional
    public PreviewVersionResponse publish(String previewId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actor = currentUserService.requiredUserId();
        PreviewVersionResponse draft = getDraft(previewId);
        validateBalancedContent(draft.content());
        Instant now = Instant.now();
        jdbcTemplate.update("""
                UPDATE realistic_job_preview_versions
                   SET status = 'ARCHIVED'
                 WHERE preview_id = CAST(:previewId AS UUID)
                   AND status = 'PUBLISHED'
                """, new MapSqlParameterSource("previewId", previewId));
        jdbcTemplate.update("""
                UPDATE realistic_job_preview_versions
                   SET status = 'PUBLISHED', published_by = :actor, published_at = :now
                 WHERE id = CAST(:versionId AS UUID)
                """, new MapSqlParameterSource()
                .addValue("actor", actor)
                .addValue("now", now)
                .addValue("versionId", draft.versionId()));
        jdbcTemplate.update("""
                UPDATE realistic_job_previews
                   SET active_version_number = :versionNumber, updated_by = :actor, updated_at = :now
                 WHERE id = CAST(:previewId AS UUID) AND empresa_id = :empresaId
                """, new MapSqlParameterSource()
                .addValue("versionNumber", draft.versionNumber())
                .addValue("actor", actor)
                .addValue("now", now)
                .addValue("previewId", previewId)
                .addValue("empresaId", empresaId));

        UUID nextVersionId = UUID.randomUUID();
        insertVersion(nextVersionId, UUID.fromString(previewId), draft.versionNumber() + 1,
                "DRAFT", draft.content(), actor, now);
        replaceScenarioLinks(nextVersionId, draft.content().scenarioNodeIds());
        return getVersion(previewId, draft.versionNumber(), empresaId);
    }

    @Transactional
    public CandidatePreviewResponse present(String token, DisplayStage stage) {
        CandidateAttemptTokenResolver.ResolvedAttemptToken resolved = candidateAttemptTokenResolver.resolve(token);
        AttemptContext attempt = findAttempt(resolved);
        CandidatePreviewResponse existing = findExistingPresentation(attempt, stage);
        if (existing != null) {
            return existing;
        }
        CandidatePreviewResponse selected = findPublishedPreview(attempt, stage);
        if (selected == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhuma prévia realista foi configurada para esta etapa.");
        }
        jdbcTemplate.update("""
                INSERT INTO attempt_realistic_preview_presentations (
                    id, empresa_id, candidate_attempt_id, preview_version_id, display_stage, presented_at
                ) VALUES (
                    :id, :empresaId, :attemptId, CAST(:versionId AS UUID), :stage, :presentedAt
                )
                ON CONFLICT (candidate_attempt_id, preview_version_id, display_stage) DO NOTHING
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("empresaId", attempt.empresaId())
                .addValue("attemptId", attempt.attemptId())
                .addValue("versionId", selected.versionId())
                .addValue("stage", stage.name())
                .addValue("presentedAt", Instant.now()));
        return selected;
    }

    @Transactional
    public void react(String token, String versionId, DisplayStage stage, CandidatePreviewReactionRequest request) {
        CandidateAttemptTokenResolver.ResolvedAttemptToken resolved = candidateAttemptTokenResolver.resolve(token);
        AttemptContext attempt = findAttempt(resolved);
        int updated = jdbcTemplate.update("""
                UPDATE attempt_realistic_preview_presentations
                   SET acknowledged_at = CASE WHEN :acknowledged THEN COALESCE(acknowledged_at, :now) ELSE acknowledged_at END,
                       voluntary_withdrawal = :voluntaryWithdrawal,
                       clarity_score = :clarityScore,
                       realism_score = :realismScore,
                       expectation_compatibility_score = :compatibilityScore,
                       reaction_recorded_at = :now
                 WHERE candidate_attempt_id = :attemptId
                   AND empresa_id = :empresaId
                   AND preview_version_id = CAST(:versionId AS UUID)
                   AND display_stage = :stage
                """, new MapSqlParameterSource()
                .addValue("acknowledged", request.acknowledged())
                .addValue("voluntaryWithdrawal", request.voluntaryWithdrawal())
                .addValue("clarityScore", request.clarityScore())
                .addValue("realismScore", request.realismScore())
                .addValue("compatibilityScore", request.expectationCompatibilityScore())
                .addValue("now", Instant.now())
                .addValue("attemptId", attempt.attemptId())
                .addValue("empresaId", attempt.empresaId())
                .addValue("versionId", versionId)
                .addValue("stage", stage.name()));
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Apresentação da prévia não encontrada.");
        }
    }

    @Transactional(readOnly = true)
    public PreviewMetricsResponse metrics(String previewId, Integer versionNumber) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        Integer resolvedVersion = versionNumber;
        if (resolvedVersion == null) {
            resolvedVersion = jdbcTemplate.queryForObject("""
                    SELECT active_version_number FROM realistic_job_previews
                     WHERE id = CAST(:previewId AS UUID) AND empresa_id = :empresaId
                    """, new MapSqlParameterSource()
                    .addValue("previewId", previewId)
                    .addValue("empresaId", empresaId), Integer.class);
        }
        if (resolvedVersion == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "A prévia ainda não possui versão publicada.");
        }
        MetricsRow metrics = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) AS presentations,
                       COUNT(acknowledged_at) AS acknowledgements,
                       SUM(CASE WHEN voluntary_withdrawal THEN 1 ELSE 0 END) AS withdrawals,
                       AVG(clarity_score) AS average_clarity,
                       AVG(realism_score) AS average_realism,
                       AVG(expectation_compatibility_score) AS average_compatibility
                  FROM attempt_realistic_preview_presentations ap
                  JOIN realistic_job_preview_versions v ON v.id = ap.preview_version_id
                  JOIN realistic_job_previews p ON p.id = v.preview_id
                 WHERE p.id = CAST(:previewId AS UUID)
                   AND p.empresa_id = :empresaId
                   AND v.version_number = :versionNumber
                """, new MapSqlParameterSource()
                .addValue("previewId", previewId)
                .addValue("empresaId", empresaId)
                .addValue("versionNumber", resolvedVersion), (resultSet, rowNumber) -> new MetricsRow(
                resultSet.getLong("presentations"),
                resultSet.getLong("acknowledgements"),
                resultSet.getLong("withdrawals"),
                resultSet.getBigDecimal("average_clarity"),
                resultSet.getBigDecimal("average_realism"),
                resultSet.getBigDecimal("average_compatibility")
        ));
        if (metrics == null) {
            metrics = new MetricsRow(0, 0, 0, null, null, null);
        }
        boolean suppressed = metrics.presentations() < MINIMUM_METRICS_SAMPLE;
        return new PreviewMetricsResponse(
                previewId,
                resolvedVersion,
                metrics.presentations(),
                metrics.acknowledgements(),
                metrics.withdrawals(),
                percent(metrics.acknowledgements(), metrics.presentations()),
                percent(metrics.withdrawals(), metrics.presentations()),
                suppressed ? null : decimal(metrics.averageClarity()),
                suppressed ? null : decimal(metrics.averageRealism()),
                suppressed ? null : decimal(metrics.averageCompatibility()),
                suppressed,
                MINIMUM_METRICS_SAMPLE,
                suppressed
                        ? "As médias foram suprimidas porque a amostra ainda é pequena."
                        : "Métricas agregadas; nenhuma pessoa é identificada."
        );
    }

    private PreviewVersionResponse getVersion(String previewId, int versionNumber, String empresaId) {
        return jdbcTemplate.queryForObject("""
                SELECT p.id AS preview_id, p.title, p.display_position, p.acknowledgement_required,
                       v.id AS version_id, v.version_number, v.status, v.responsibilities, v.autonomy,
                       v.pressure_context, v.contact_frequency, v.critical_situations,
                       v.routine_description, v.work_conditions, v.positive_aspects,
                       v.media_json, v.alternative_text, v.published_at
                  FROM realistic_job_previews p
                  JOIN realistic_job_preview_versions v ON v.preview_id = p.id
                 WHERE p.id = CAST(:previewId AS UUID)
                   AND p.empresa_id = :empresaId
                   AND v.version_number = :versionNumber
                """, new MapSqlParameterSource()
                .addValue("previewId", previewId)
                .addValue("empresaId", empresaId)
                .addValue("versionNumber", versionNumber),
                (resultSet, rowNumber) -> mapVersion(resultSet));
    }

    private CandidatePreviewResponse findExistingPresentation(AttemptContext attempt, DisplayStage stage) {
        List<CandidatePreviewResponse> rows = jdbcTemplate.query("""
                SELECT p.id AS preview_id, p.title, p.acknowledgement_required,
                       v.id AS version_id, v.version_number, v.responsibilities, v.autonomy,
                       v.pressure_context, v.contact_frequency, v.critical_situations,
                       v.routine_description, v.work_conditions, v.positive_aspects,
                       v.media_json, v.alternative_text
                  FROM attempt_realistic_preview_presentations ap
                  JOIN realistic_job_preview_versions v ON v.id = ap.preview_version_id
                  JOIN realistic_job_previews p ON p.id = v.preview_id
                 WHERE ap.candidate_attempt_id = :attemptId
                   AND ap.empresa_id = :empresaId
                   AND ap.display_stage = :stage
                 ORDER BY ap.presented_at DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("attemptId", attempt.attemptId())
                .addValue("empresaId", attempt.empresaId())
                .addValue("stage", stage.name()),
                (resultSet, rowNumber) -> mapCandidatePreview(resultSet, stage));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private CandidatePreviewResponse findPublishedPreview(AttemptContext attempt, DisplayStage stage) {
        List<CandidatePreviewResponse> rows = jdbcTemplate.query("""
                SELECT p.id AS preview_id, p.title, p.acknowledgement_required,
                       v.id AS version_id, v.version_number, v.responsibilities, v.autonomy,
                       v.pressure_context, v.contact_frequency, v.critical_situations,
                       v.routine_description, v.work_conditions, v.positive_aspects,
                       v.media_json, v.alternative_text
                  FROM realistic_job_previews p
                  JOIN realistic_job_preview_versions v
                    ON v.preview_id = p.id AND v.version_number = p.active_version_number
                 WHERE p.empresa_id = :empresaId
                   AND v.status = 'PUBLISHED'
                   AND p.display_position IN (:stage, 'BOTH')
                   AND (
                        (p.scope_type = 'JOB' AND p.scope_key = :jobId)
                        OR (p.scope_type = 'JOURNEY' AND p.scope_key = :simulationId)
                   )
                 ORDER BY CASE WHEN p.scope_type = 'JOB' THEN 0 ELSE 1 END, p.updated_at DESC
                 LIMIT 1
                """, new MapSqlParameterSource()
                .addValue("empresaId", attempt.empresaId())
                .addValue("stage", stage.name())
                .addValue("jobId", attempt.gupyJobId() == null ? "" : attempt.gupyJobId().toString())
                .addValue("simulationId", attempt.simulationId()),
                (resultSet, rowNumber) -> mapCandidatePreview(resultSet, stage));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private AttemptContext findAttempt(CandidateAttemptTokenResolver.ResolvedAttemptToken resolved) {
        List<AttemptContext> rows = jdbcTemplate.query("""
                SELECT id, empresa_id, simulation_id, gupy_job_id
                  FROM candidate_attempts
                 WHERE id = :attemptId
                   AND (:empresaId IS NULL OR empresa_id = :empresaId)
                """, new MapSqlParameterSource()
                .addValue("attemptId", resolved.attemptId())
                .addValue("empresaId", resolved.empresaId()), (resultSet, rowNumber) -> new AttemptContext(
                resultSet.getString("id"),
                resultSet.getString("empresa_id"),
                resultSet.getString("simulation_id"),
                resultSet.getObject("gupy_job_id", Long.class)
        ));
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participação não encontrada.");
        }
        return rows.getFirst();
    }

    private PreviewVersionResponse mapVersion(ResultSet resultSet) throws SQLException {
        PreviewContentRequest content = mapContent(resultSet,
                scenarioIds(resultSet.getString("version_id")));
        return new PreviewVersionResponse(
                resultSet.getString("preview_id"),
                resultSet.getString("version_id"),
                resultSet.getInt("version_number"),
                resultSet.getString("status"),
                resultSet.getString("title"),
                DisplayPosition.valueOf(resultSet.getString("display_position")),
                resultSet.getBoolean("acknowledgement_required"),
                content,
                toInstant(resultSet.getTimestamp("published_at"))
        );
    }

    private CandidatePreviewResponse mapCandidatePreview(ResultSet resultSet, DisplayStage stage) throws SQLException {
        return new CandidatePreviewResponse(
                resultSet.getString("preview_id"),
                resultSet.getString("version_id"),
                resultSet.getInt("version_number"),
                resultSet.getString("title"),
                stage,
                resultSet.getBoolean("acknowledgement_required"),
                mapContent(resultSet, scenarioIds(resultSet.getString("version_id"))),
                INFORMATIONAL_NOTICE
        );
    }

    private PreviewContentRequest mapContent(ResultSet resultSet, List<String> scenarioIds) throws SQLException {
        return new PreviewContentRequest(
                resultSet.getString("responsibilities"),
                resultSet.getString("autonomy"),
                resultSet.getString("pressure_context"),
                resultSet.getString("contact_frequency"),
                resultSet.getString("critical_situations"),
                resultSet.getString("routine_description"),
                resultSet.getString("work_conditions"),
                resultSet.getString("positive_aspects"),
                resultSet.getString("alternative_text"),
                readMedia(resultSet.getString("media_json")),
                scenarioIds
        );
    }

    private List<String> scenarioIds(String versionId) {
        return jdbcTemplate.queryForList("""
                SELECT scenario_node_id
                  FROM realistic_job_preview_scenarios
                 WHERE preview_version_id = CAST(:versionId AS UUID)
                 ORDER BY scenario_node_id
                """, new MapSqlParameterSource("versionId", versionId), String.class);
    }

    private void insertVersion(
            UUID versionId,
            UUID previewId,
            int versionNumber,
            String status,
            PreviewContentRequest content,
            String actor,
            Instant now
    ) {
        jdbcTemplate.update("""
                INSERT INTO realistic_job_preview_versions (
                    id, preview_id, version_number, status, responsibilities, autonomy,
                    pressure_context, contact_frequency, critical_situations, routine_description,
                    work_conditions, positive_aspects, media_json, alternative_text, created_by, created_at
                ) VALUES (
                    :id, :previewId, :versionNumber, :status, :responsibilities, :autonomy,
                    :pressureContext, :contactFrequency, :criticalSituations, :routineDescription,
                    :workConditions, :positiveAspects, :mediaJson, :alternativeText, :actor, :now
                )
                """, versionParameters(versionId, previewId, versionNumber, status, content, actor, now));
    }

    private void updateVersion(String versionId, PreviewContentRequest content) {
        MapSqlParameterSource parameters = contentParameters(content).addValue("versionId", versionId);
        jdbcTemplate.update("""
                UPDATE realistic_job_preview_versions
                   SET responsibilities = :responsibilities,
                       autonomy = :autonomy,
                       pressure_context = :pressureContext,
                       contact_frequency = :contactFrequency,
                       critical_situations = :criticalSituations,
                       routine_description = :routineDescription,
                       work_conditions = :workConditions,
                       positive_aspects = :positiveAspects,
                       media_json = :mediaJson,
                       alternative_text = :alternativeText
                 WHERE id = CAST(:versionId AS UUID) AND status = 'DRAFT'
                """, parameters);
    }

    private MapSqlParameterSource versionParameters(
            UUID versionId,
            UUID previewId,
            int versionNumber,
            String status,
            PreviewContentRequest content,
            String actor,
            Instant now
    ) {
        return contentParameters(content)
                .addValue("id", versionId)
                .addValue("previewId", previewId)
                .addValue("versionNumber", versionNumber)
                .addValue("status", status)
                .addValue("actor", actor)
                .addValue("now", now);
    }

    private MapSqlParameterSource contentParameters(PreviewContentRequest content) {
        return new MapSqlParameterSource()
                .addValue("responsibilities", content.responsibilities().trim())
                .addValue("autonomy", content.autonomy().trim())
                .addValue("pressureContext", content.pressureContext().trim())
                .addValue("contactFrequency", content.contactFrequency().trim())
                .addValue("criticalSituations", content.criticalSituations().trim())
                .addValue("routineDescription", content.routineDescription().trim())
                .addValue("workConditions", content.workConditions().trim())
                .addValue("positiveAspects", content.positiveAspects().trim())
                .addValue("mediaJson", writeMedia(content.media()))
                .addValue("alternativeText", content.alternativeText().trim());
    }

    private void replaceScenarioLinks(UUID versionId, List<String> scenarioNodeIds) {
        jdbcTemplate.update("DELETE FROM realistic_job_preview_scenarios WHERE preview_version_id = :versionId",
                new MapSqlParameterSource("versionId", versionId));
        if (scenarioNodeIds == null) {
            return;
        }
        for (String scenarioNodeId : scenarioNodeIds.stream().distinct().toList()) {
            jdbcTemplate.update("""
                    INSERT INTO realistic_job_preview_scenarios (preview_version_id, scenario_node_id)
                    VALUES (:versionId, :scenarioNodeId)
                    """, new MapSqlParameterSource()
                    .addValue("versionId", versionId)
                    .addValue("scenarioNodeId", scenarioNodeId.trim()));
        }
    }

    private void validateBalancedContent(PreviewContentRequest content) {
        if (content == null || content.positiveAspects().isBlank()
                || content.pressureContext().isBlank()
                || content.criticalSituations().isBlank()
                || content.workConditions().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A prévia deve apresentar aspectos positivos, pressão, situações críticas e condições reais de trabalho.");
        }
        if (content.media() != null) {
            content.media().forEach(media -> {
                if (media.alternativeText().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Toda mídia precisa de alternativa textual.");
                }
            });
        }
    }

    private String writeMedia(List<MediaItem> media) {
        try {
            return objectMapper.writeValueAsString(media == null ? List.of() : media);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível serializar as mídias.");
        }
    }

    private List<MediaItem> readMedia(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<MediaItem>>() { });
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Mídias da prévia estão inválidas.", exception);
        }
    }

    private double percent(long numerator, long denominator) {
        if (denominator == 0) {
            return 0;
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private Double decimal(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private record AttemptContext(String attemptId, String empresaId, String simulationId, Long gupyJobId) {
    }

    private record MetricsRow(
            long presentations,
            long acknowledgements,
            long withdrawals,
            BigDecimal averageClarity,
            BigDecimal averageRealism,
            BigDecimal averageCompatibility
    ) {
    }
}
