package br.com.iforce.praxis.localization.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.CompetencyTranslation;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.ConfigureLocalesRequest;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.ExportLocalePackageResponse;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.ImportLocalePackageRequest;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleContentRequest;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleContentResponse;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleStatus;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleSummaryResponse;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.NodeTranslation;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.OptionTranslation;
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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class SimulationLocalizationService {

    private static final String EXPORT_SCHEMA_VERSION = "praxis-simulation-locale-v1";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;

    public SimulationLocalizationService(
            NamedParameterJdbcTemplate jdbcTemplate,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public List<LocaleSummaryResponse> configure(long simulationVersionId, ConfigureLocalesRequest request) {
        VersionContext context = requireVersion(simulationVersionId);
        String actor = currentUserService.requiredUserId();
        String baseLocale = normalizeLocale(request.baseLocale());
        LinkedHashSet<String> enabled = new LinkedHashSet<>();
        enabled.add(baseLocale);
        request.enabledLocales().forEach(locale -> enabled.add(normalizeLocale(locale)));

        jdbcTemplate.update("""
                UPDATE simulation_version_locales
                   SET is_base_locale = FALSE, updated_by = :actor, updated_at = :now
                 WHERE simulation_version_id = :versionId
                """, new MapSqlParameterSource()
                .addValue("versionId", simulationVersionId)
                .addValue("actor", actor)
                .addValue("now", Instant.now()));

        LocaleContentRequest baseContent = loadSourceContent(context);
        for (String locale : enabled) {
            boolean base = locale.equals(baseLocale);
            UUID localeId = ensureLocale(simulationVersionId, locale, base, actor);
            if (!hasLocaleContent(localeId)) {
                saveContent(localeId, baseContent);
            }
            if (base) {
                jdbcTemplate.update("""
                        UPDATE simulation_version_locales
                           SET status = 'APPROVED', reviewed_by = :actor, reviewed_at = :now,
                               approved_by = :actor, approved_at = :now
                         WHERE id = :localeId
                        """, new MapSqlParameterSource()
                        .addValue("localeId", localeId)
                        .addValue("actor", actor)
                        .addValue("now", Instant.now()));
            }
        }

        jdbcTemplate.update("""
                DELETE FROM simulation_version_locales
                 WHERE simulation_version_id = :versionId
                   AND locale NOT IN (:enabledLocales)
                   AND is_base_locale = FALSE
                """, new MapSqlParameterSource()
                .addValue("versionId", simulationVersionId)
                .addValue("enabledLocales", enabled));
        return list(simulationVersionId);
    }

    @Transactional(readOnly = true)
    public List<LocaleSummaryResponse> list(long simulationVersionId) {
        VersionContext context = requireVersion(simulationVersionId);
        Counts totals = graphCounts(simulationVersionId);
        return jdbcTemplate.query("""
                SELECT l.locale, l.is_base_locale, l.status, l.revision, l.updated_at,
                       l.reviewed_by, l.approved_by,
                       (SELECT COUNT(*) FROM simulation_node_translations nt WHERE nt.locale_id = l.id) AS translated_nodes,
                       (SELECT COUNT(*) FROM simulation_option_translations ot WHERE ot.locale_id = l.id) AS translated_options,
                       (SELECT COUNT(*) FROM simulation_competency_translations ct WHERE ct.locale_id = l.id) AS translated_competencies
                  FROM simulation_version_locales l
                 WHERE l.simulation_version_id = :versionId
                 ORDER BY l.is_base_locale DESC, l.locale
                """, new MapSqlParameterSource("versionId", context.versionId()),
                (resultSet, rowNumber) -> {
                    long translatedNodes = resultSet.getLong("translated_nodes");
                    long translatedOptions = resultSet.getLong("translated_options");
                    long translatedCompetencies = resultSet.getLong("translated_competencies");
                    long totalElements = totals.nodes() + totals.options() + totals.competencies();
                    long translatedElements = translatedNodes + translatedOptions + translatedCompetencies;
                    double completeness = totalElements == 0
                            ? 100.0
                            : round(translatedElements * 100.0 / totalElements, 1);
                    return new LocaleSummaryResponse(
                            resultSet.getString("locale"),
                            resultSet.getBoolean("is_base_locale"),
                            LocaleStatus.valueOf(resultSet.getString("status")),
                            resultSet.getInt("revision"),
                            translatedNodes,
                            totals.nodes(),
                            translatedOptions,
                            totals.options(),
                            translatedCompetencies,
                            totals.competencies(),
                            completeness,
                            resultSet.getTimestamp("updated_at").toInstant(),
                            resultSet.getString("reviewed_by"),
                            resultSet.getString("approved_by")
                    );
                });
    }

    @Transactional(readOnly = true)
    public LocaleContentResponse get(long simulationVersionId, String locale) {
        requireVersion(simulationVersionId);
        LocaleRecord localeRecord = requireLocale(simulationVersionId, locale);
        LocaleContentRequest content = loadContent(localeRecord.id());
        ValidationResult validation = validateStructure(simulationVersionId, content);
        return new LocaleContentResponse(
                simulationVersionId,
                localeRecord.locale(),
                localeRecord.baseLocale(),
                localeRecord.status(),
                localeRecord.revision(),
                content,
                validation.errors(),
                validation.warnings()
        );
    }

    @Transactional
    public LocaleContentResponse save(long simulationVersionId, String locale, LocaleContentRequest content) {
        requireVersion(simulationVersionId);
        LocaleRecord localeRecord = requireLocale(simulationVersionId, locale);
        if (localeRecord.baseLocale()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O idioma base é atualizado pelo conteúdo original da versão, não pelo editor de tradução.");
        }
        if (localeRecord.status() == LocaleStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Conteúdo aprovado é imutável. Crie uma nova versão da avaliação para alterar a tradução.");
        }
        ValidationResult validation = validateStructure(simulationVersionId, content);
        if (!validation.errors().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.join(" ", validation.errors()));
        }
        saveContent(localeRecord.id(), content);
        jdbcTemplate.update("""
                UPDATE simulation_version_locales
                   SET status = 'DRAFT', revision = revision + 1,
                       updated_by = :actor, updated_at = :now,
                       reviewed_by = NULL, reviewed_at = NULL,
                       approved_by = NULL, approved_at = NULL
                 WHERE id = :localeId
                """, new MapSqlParameterSource()
                .addValue("localeId", localeRecord.id())
                .addValue("actor", currentUserService.requiredUserId())
                .addValue("now", Instant.now()));
        return get(simulationVersionId, locale);
    }

    @Transactional
    public LocaleContentResponse submitForReview(long simulationVersionId, String locale) {
        LocaleContentResponse response = get(simulationVersionId, locale);
        if (response.baseLocale()) {
            return response;
        }
        if (!response.validationErrors().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.join(" ", response.validationErrors()));
        }
        jdbcTemplate.update("""
                UPDATE simulation_version_locales
                   SET status = 'IN_REVIEW', reviewed_by = :actor, reviewed_at = :now,
                       updated_by = :actor, updated_at = :now
                 WHERE simulation_version_id = :versionId AND locale = :locale
                """, localeParameters(simulationVersionId, locale)
                .addValue("actor", currentUserService.requiredUserId())
                .addValue("now", Instant.now()));
        return get(simulationVersionId, locale);
    }

    @Transactional
    public LocaleContentResponse approve(long simulationVersionId, String locale) {
        LocaleContentResponse response = get(simulationVersionId, locale);
        if (!response.validationErrors().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    String.join(" ", response.validationErrors()));
        }
        if (!response.baseLocale() && response.status() != LocaleStatus.IN_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A tradução precisa estar em revisão antes da aprovação.");
        }
        jdbcTemplate.update("""
                UPDATE simulation_version_locales
                   SET status = 'APPROVED', approved_by = :actor, approved_at = :now,
                       updated_by = :actor, updated_at = :now
                 WHERE simulation_version_id = :versionId AND locale = :locale
                """, localeParameters(simulationVersionId, locale)
                .addValue("actor", currentUserService.requiredUserId())
                .addValue("now", Instant.now()));
        return get(simulationVersionId, locale);
    }

    @Transactional(readOnly = true)
    public ExportLocalePackageResponse exportPackage(long simulationVersionId, String locale) {
        LocaleContentResponse response = get(simulationVersionId, locale);
        return new ExportLocalePackageResponse(
                EXPORT_SCHEMA_VERSION,
                simulationVersionId,
                response.locale(),
                response.baseLocale(),
                response.status(),
                response.revision(),
                response.content(),
                Instant.now()
        );
    }

    @Transactional
    public LocaleContentResponse importPackage(long simulationVersionId, ImportLocalePackageRequest request) {
        requireVersion(simulationVersionId);
        String locale = normalizeLocale(request.locale());
        LocaleRecord existing = findLocale(simulationVersionId, locale);
        if (existing == null) {
            ensureLocale(simulationVersionId, locale, false, currentUserService.requiredUserId());
        } else if (!request.replaceExisting()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O idioma já existe. Confirme a substituição do rascunho.");
        }
        LocaleRecord localeRecord = requireLocale(simulationVersionId, locale);
        if (localeRecord.baseLocale() || localeRecord.status() == LocaleStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é permitido substituir o idioma base ou uma tradução aprovada.");
        }
        return save(simulationVersionId, locale, request.content());
    }

    private VersionContext requireVersion(long simulationVersionId) {
        List<VersionContext> rows = jdbcTemplate.query("""
                SELECT sv.id, s.id AS simulation_id, s.empresa_id, s.name, s.description
                  FROM simulation_versions sv
                  JOIN simulations s ON s.id = sv.simulation_id
                 WHERE sv.id = :versionId AND s.empresa_id = :empresaId
                """, new MapSqlParameterSource()
                .addValue("versionId", simulationVersionId)
                .addValue("empresaId", currentEmpresaService.requiredEmpresaId()),
                (resultSet, rowNumber) -> new VersionContext(
                        resultSet.getLong("id"),
                        resultSet.getString("simulation_id"),
                        resultSet.getString("empresa_id"),
                        resultSet.getString("name"),
                        resultSet.getString("description")
                ));
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Versão da avaliação não encontrada na empresa atual.");
        }
        return rows.getFirst();
    }

    private LocaleContentRequest loadSourceContent(VersionContext context) {
        List<NodeTranslation> nodes = jdbcTemplate.query("""
                SELECT node_id, speaker, message, plain_text_description, media_transcript
                  FROM simulation_nodes
                 WHERE simulation_version_id = :versionId
                 ORDER BY display_order, node_id
                """, new MapSqlParameterSource("versionId", context.versionId()),
                (resultSet, rowNumber) -> new NodeTranslation(
                        resultSet.getString("node_id"),
                        resultSet.getString("speaker"),
                        resultSet.getString("message"),
                        null,
                        resultSet.getString("plain_text_description"),
                        resultSet.getString("media_transcript")
                ));
        List<OptionTranslation> options = jdbcTemplate.query("""
                SELECT n.node_id, o.option_id, o.text, o.plain_text_description, o.media_transcript
                  FROM simulation_options o
                  JOIN simulation_nodes n ON n.id = o.simulation_node_id
                 WHERE n.simulation_version_id = :versionId
                 ORDER BY n.display_order, o.display_order, o.option_id
                """, new MapSqlParameterSource("versionId", context.versionId()),
                (resultSet, rowNumber) -> new OptionTranslation(
                        resultSet.getString("node_id"),
                        resultSet.getString("option_id"),
                        resultSet.getString("text"),
                        resultSet.getString("plain_text_description"),
                        resultSet.getString("media_transcript")
                ));
        List<CompetencyTranslation> competencies = jdbcTemplate.query("""
                SELECT name FROM simulation_competencies
                 WHERE simulation_version_id = :versionId
                 ORDER BY name
                """, new MapSqlParameterSource("versionId", context.versionId()),
                (resultSet, rowNumber) -> new CompetencyTranslation(
                        resultSet.getString("name"),
                        resultSet.getString("name"),
                        resultSet.getString("name")
                ));
        return new LocaleContentRequest(
                context.name(),
                context.description(),
                context.description(),
                context.description(),
                nodes,
                options,
                competencies
        );
    }

    private UUID ensureLocale(long versionId, String locale, boolean baseLocale, String actor) {
        LocaleRecord existing = findLocale(versionId, locale);
        if (existing != null) {
            jdbcTemplate.update("""
                    UPDATE simulation_version_locales
                       SET is_base_locale = :baseLocale, updated_by = :actor, updated_at = :now
                     WHERE id = :localeId
                    """, new MapSqlParameterSource()
                    .addValue("baseLocale", baseLocale)
                    .addValue("actor", actor)
                    .addValue("now", Instant.now())
                    .addValue("localeId", existing.id()));
            return existing.id();
        }
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO simulation_version_locales (
                    id, simulation_version_id, locale, is_base_locale, status, revision,
                    created_by, created_at, updated_by, updated_at
                ) VALUES (
                    :id, :versionId, :locale, :baseLocale, 'DRAFT', 1,
                    :actor, :now, :actor, :now
                )
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("versionId", versionId)
                .addValue("locale", locale)
                .addValue("baseLocale", baseLocale)
                .addValue("actor", actor)
                .addValue("now", Instant.now()));
        return id;
    }

    private boolean hasLocaleContent(UUID localeId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM simulation_locale_contents WHERE locale_id = :localeId",
                new MapSqlParameterSource("localeId", localeId), Long.class);
        return count != null && count > 0;
    }

    private void saveContent(UUID localeId, LocaleContentRequest content) {
        jdbcTemplate.update("""
                INSERT INTO simulation_locale_contents (
                    locale_id, title, description, instructions, report_introduction
                ) VALUES (
                    :localeId, :title, :description, :instructions, :reportIntroduction
                )
                ON CONFLICT (locale_id) DO UPDATE SET
                    title = EXCLUDED.title,
                    description = EXCLUDED.description,
                    instructions = EXCLUDED.instructions,
                    report_introduction = EXCLUDED.report_introduction
                """, new MapSqlParameterSource()
                .addValue("localeId", localeId)
                .addValue("title", content.title().trim())
                .addValue("description", content.description().trim())
                .addValue("instructions", content.instructions().trim())
                .addValue("reportIntroduction", content.reportIntroduction().trim()));

        jdbcTemplate.update("DELETE FROM simulation_node_translations WHERE locale_id = :localeId",
                new MapSqlParameterSource("localeId", localeId));
        for (NodeTranslation node : content.nodes()) {
            jdbcTemplate.update("""
                    INSERT INTO simulation_node_translations (
                        locale_id, node_id, speaker, message, report_text,
                        plain_text_description, media_transcript
                    ) VALUES (
                        :localeId, :nodeId, :speaker, :message, :reportText,
                        :plainTextDescription, :mediaTranscript
                    )
                    """, new MapSqlParameterSource()
                    .addValue("localeId", localeId)
                    .addValue("nodeId", node.nodeId())
                    .addValue("speaker", node.speaker().trim())
                    .addValue("message", node.message().trim())
                    .addValue("reportText", trimToNull(node.reportText()))
                    .addValue("plainTextDescription", trimToNull(node.plainTextDescription()))
                    .addValue("mediaTranscript", trimToNull(node.mediaTranscript())));
        }

        jdbcTemplate.update("DELETE FROM simulation_option_translations WHERE locale_id = :localeId",
                new MapSqlParameterSource("localeId", localeId));
        for (OptionTranslation option : content.options()) {
            jdbcTemplate.update("""
                    INSERT INTO simulation_option_translations (
                        locale_id, node_id, option_id, text, plain_text_description, media_transcript
                    ) VALUES (
                        :localeId, :nodeId, :optionId, :text, :plainTextDescription, :mediaTranscript
                    )
                    """, new MapSqlParameterSource()
                    .addValue("localeId", localeId)
                    .addValue("nodeId", option.nodeId())
                    .addValue("optionId", option.optionId())
                    .addValue("text", option.text().trim())
                    .addValue("plainTextDescription", trimToNull(option.plainTextDescription()))
                    .addValue("mediaTranscript", trimToNull(option.mediaTranscript())));
        }

        jdbcTemplate.update("DELETE FROM simulation_competency_translations WHERE locale_id = :localeId",
                new MapSqlParameterSource("localeId", localeId));
        for (CompetencyTranslation competency : content.competencies()) {
            jdbcTemplate.update("""
                    INSERT INTO simulation_competency_translations (
                        locale_id, competency_name, display_name, report_text
                    ) VALUES (
                        :localeId, :competencyName, :displayName, :reportText
                    )
                    """, new MapSqlParameterSource()
                    .addValue("localeId", localeId)
                    .addValue("competencyName", competency.competencyName())
                    .addValue("displayName", competency.displayName().trim())
                    .addValue("reportText", competency.reportText().trim()));
        }
    }

    private LocaleContentRequest loadContent(UUID localeId) {
        HeaderContent header = jdbcTemplate.queryForObject("""
                SELECT title, description, instructions, report_introduction
                  FROM simulation_locale_contents
                 WHERE locale_id = :localeId
                """, new MapSqlParameterSource("localeId", localeId),
                (resultSet, rowNumber) -> new HeaderContent(
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getString("instructions"),
                        resultSet.getString("report_introduction")
                ));
        if (header == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo do idioma não encontrado.");
        }
        List<NodeTranslation> nodes = jdbcTemplate.query("""
                SELECT node_id, speaker, message, report_text, plain_text_description, media_transcript
                  FROM simulation_node_translations
                 WHERE locale_id = :localeId
                 ORDER BY node_id
                """, new MapSqlParameterSource("localeId", localeId),
                (resultSet, rowNumber) -> new NodeTranslation(
                        resultSet.getString("node_id"),
                        resultSet.getString("speaker"),
                        resultSet.getString("message"),
                        resultSet.getString("report_text"),
                        resultSet.getString("plain_text_description"),
                        resultSet.getString("media_transcript")
                ));
        List<OptionTranslation> options = jdbcTemplate.query("""
                SELECT node_id, option_id, text, plain_text_description, media_transcript
                  FROM simulation_option_translations
                 WHERE locale_id = :localeId
                 ORDER BY node_id, option_id
                """, new MapSqlParameterSource("localeId", localeId),
                (resultSet, rowNumber) -> new OptionTranslation(
                        resultSet.getString("node_id"),
                        resultSet.getString("option_id"),
                        resultSet.getString("text"),
                        resultSet.getString("plain_text_description"),
                        resultSet.getString("media_transcript")
                ));
        List<CompetencyTranslation> competencies = jdbcTemplate.query("""
                SELECT competency_name, display_name, report_text
                  FROM simulation_competency_translations
                 WHERE locale_id = :localeId
                 ORDER BY competency_name
                """, new MapSqlParameterSource("localeId", localeId),
                (resultSet, rowNumber) -> new CompetencyTranslation(
                        resultSet.getString("competency_name"),
                        resultSet.getString("display_name"),
                        resultSet.getString("report_text")
                ));
        return new LocaleContentRequest(
                header.title(),
                header.description(),
                header.instructions(),
                header.reportIntroduction(),
                nodes,
                options,
                competencies
        );
    }

    private ValidationResult validateStructure(long versionId, LocaleContentRequest content) {
        Set<String> expectedNodes = new LinkedHashSet<>(jdbcTemplate.queryForList("""
                SELECT node_id FROM simulation_nodes
                 WHERE simulation_version_id = :versionId
                 ORDER BY node_id
                """, new MapSqlParameterSource("versionId", versionId), String.class));
        Set<String> expectedOptions = new LinkedHashSet<>(jdbcTemplate.queryForList("""
                SELECT n.node_id || ':' || o.option_id
                  FROM simulation_options o
                  JOIN simulation_nodes n ON n.id = o.simulation_node_id
                 WHERE n.simulation_version_id = :versionId
                 ORDER BY n.node_id, o.option_id
                """, new MapSqlParameterSource("versionId", versionId), String.class));
        Set<String> expectedCompetencies = new LinkedHashSet<>(jdbcTemplate.queryForList("""
                SELECT name FROM simulation_competencies
                 WHERE simulation_version_id = :versionId
                 ORDER BY name
                """, new MapSqlParameterSource("versionId", versionId), String.class));

        Set<String> translatedNodes = new LinkedHashSet<>();
        Set<String> duplicateNodes = new HashSet<>();
        for (NodeTranslation node : content.nodes()) {
            if (!translatedNodes.add(node.nodeId())) {
                duplicateNodes.add(node.nodeId());
            }
        }
        Set<String> translatedOptions = new LinkedHashSet<>();
        Set<String> duplicateOptions = new HashSet<>();
        for (OptionTranslation option : content.options()) {
            String key = option.nodeId() + ":" + option.optionId();
            if (!translatedOptions.add(key)) {
                duplicateOptions.add(key);
            }
        }
        Set<String> translatedCompetencies = new LinkedHashSet<>();
        Set<String> duplicateCompetencies = new HashSet<>();
        for (CompetencyTranslation competency : content.competencies()) {
            if (!translatedCompetencies.add(competency.competencyName())) {
                duplicateCompetencies.add(competency.competencyName());
            }
        }

        List<String> errors = new ArrayList<>();
        appendDifference(errors, "Nós ausentes", expectedNodes, translatedNodes);
        appendDifference(errors, "Nós desconhecidos", translatedNodes, expectedNodes);
        appendDifference(errors, "Alternativas ausentes", expectedOptions, translatedOptions);
        appendDifference(errors, "Alternativas desconhecidas", translatedOptions, expectedOptions);
        appendDifference(errors, "Competências ausentes", expectedCompetencies, translatedCompetencies);
        appendDifference(errors, "Competências desconhecidas", translatedCompetencies, expectedCompetencies);
        if (!duplicateNodes.isEmpty()) errors.add("Nós duplicados: " + String.join(", ", duplicateNodes) + ".");
        if (!duplicateOptions.isEmpty()) errors.add("Alternativas duplicadas: " + String.join(", ", duplicateOptions) + ".");
        if (!duplicateCompetencies.isEmpty()) errors.add("Competências duplicadas: " + String.join(", ", duplicateCompetencies) + ".");

        List<String> warnings = new ArrayList<>();
        long missingNodeAccessibility = content.nodes().stream()
                .filter(node -> isBlank(node.plainTextDescription()) && isBlank(node.mediaTranscript()))
                .count();
        long missingOptionAccessibility = content.options().stream()
                .filter(option -> isBlank(option.plainTextDescription()) && isBlank(option.mediaTranscript()))
                .count();
        if (missingNodeAccessibility > 0) {
            warnings.add(missingNodeAccessibility + " nós não possuem descrição textual nem transcrição específica do idioma.");
        }
        if (missingOptionAccessibility > 0) {
            warnings.add(missingOptionAccessibility + " alternativas não possuem descrição textual nem transcrição específica do idioma.");
        }
        return new ValidationResult(errors, warnings);
    }

    private void appendDifference(List<String> errors, String label, Set<String> left, Set<String> right) {
        LinkedHashSet<String> difference = new LinkedHashSet<>(left);
        difference.removeAll(right);
        if (!difference.isEmpty()) {
            errors.add(label + ": " + String.join(", ", difference) + ".");
        }
    }

    private Counts graphCounts(long versionId) {
        return new Counts(
                count("SELECT COUNT(*) FROM simulation_nodes WHERE simulation_version_id = :versionId", versionId),
                count("""
                        SELECT COUNT(*) FROM simulation_options o
                        JOIN simulation_nodes n ON n.id = o.simulation_node_id
                        WHERE n.simulation_version_id = :versionId
                        """, versionId),
                count("SELECT COUNT(*) FROM simulation_competencies WHERE simulation_version_id = :versionId", versionId)
        );
    }

    private long count(String sql, long versionId) {
        Long value = jdbcTemplate.queryForObject(sql,
                new MapSqlParameterSource("versionId", versionId), Long.class);
        return value == null ? 0 : value;
    }

    private LocaleRecord requireLocale(long versionId, String locale) {
        LocaleRecord record = findLocale(versionId, normalizeLocale(locale));
        if (record == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Idioma não habilitado para esta versão.");
        }
        return record;
    }

    private LocaleRecord findLocale(long versionId, String locale) {
        List<LocaleRecord> rows = jdbcTemplate.query("""
                SELECT id, locale, is_base_locale, status, revision
                  FROM simulation_version_locales
                 WHERE simulation_version_id = :versionId AND locale = :locale
                """, localeParameters(versionId, locale),
                (resultSet, rowNumber) -> new LocaleRecord(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("locale"),
                        resultSet.getBoolean("is_base_locale"),
                        LocaleStatus.valueOf(resultSet.getString("status")),
                        resultSet.getInt("revision")
                ));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private MapSqlParameterSource localeParameters(long versionId, String locale) {
        return new MapSqlParameterSource()
                .addValue("versionId", versionId)
                .addValue("locale", normalizeLocale(locale));
    }

    private String normalizeLocale(String locale) {
        String value = locale.trim();
        String[] parts = value.split("-", 2);
        return parts.length == 1
                ? parts[0].toLowerCase(Locale.ROOT)
                : parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }

    private record VersionContext(
            long versionId,
            String simulationId,
            String empresaId,
            String name,
            String description
    ) {
    }

    private record LocaleRecord(
            UUID id,
            String locale,
            boolean baseLocale,
            LocaleStatus status,
            int revision
    ) {
    }

    private record Counts(long nodes, long options, long competencies) {
    }

    private record HeaderContent(
            String title,
            String description,
            String instructions,
            String reportIntroduction
    ) {
    }

    private record ValidationResult(List<String> errors, List<String> warnings) {
    }
}
