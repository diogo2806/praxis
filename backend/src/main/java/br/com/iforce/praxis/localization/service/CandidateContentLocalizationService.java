package br.com.iforce.praxis.localization.service;

import br.com.iforce.praxis.candidate.dto.CandidateResultItemResponse;
import br.com.iforce.praxis.candidate.dto.CandidateResultPageResponse;
import br.com.iforce.praxis.candidate.dto.EtapaAtualResponse;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.candidate.dto.RespostaResponse;
import br.com.iforce.praxis.candidate.service.CandidateAttemptTokenResolver;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleSelectionRequest;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleSelectionResponse;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class CandidateContentLocalizationService {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final CandidateAttemptTokenResolver tokenResolver;

    public CandidateContentLocalizationService(
            NamedParameterJdbcTemplate jdbcTemplate,
            CandidateAttemptTokenResolver tokenResolver
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.tokenResolver = tokenResolver;
    }

    @Transactional
    public LocaleSelectionResponse selectLocale(String token, LocaleSelectionRequest request) {
        AttemptLocaleContext attempt = findAttempt(token);
        LocaleResolution resolution = resolveLocale(attempt, request.locale());
        persistSelection(attempt, resolution.selectedLocale(), request.source(), resolution.fallbackApplied());
        return new LocaleSelectionResponse(
                normalizeLocale(request.locale()),
                resolution.selectedLocale(),
                resolution.fallbackApplied() ? LocaleSource.BASE_FALLBACK : request.source(),
                resolution.fallbackApplied(),
                resolution.availableLocales()
        );
    }

    @Transactional
    public ParticipacaoResponse localizeParticipation(
            String token,
            String requestedLocale,
            ParticipacaoResponse response
    ) {
        AttemptLocaleContext attempt = findAttempt(token);
        LocaleResolution resolution = resolveAndPersist(attempt, requestedLocale);
        LocaleBundle bundle = loadBundle(attempt.versionId(), resolution.selectedLocale());
        if (bundle == null) {
            return response;
        }
        return new ParticipacaoResponse(
                response.participacaoId(),
                defaultIfBlank(bundle.title(), response.avaliacaoNome()),
                response.status(),
                response.finalizado(),
                response.redirectUrl(),
                response.acaoSugeridaFrontend(),
                response.progresso(),
                localizeStage(response.etapaAtual(), bundle),
                response.verticalSaude()
        );
    }

    @Transactional
    public RegistrarRespostaResponse localizeAnswerResponse(
            String token,
            String requestedLocale,
            RegistrarRespostaResponse response
    ) {
        AttemptLocaleContext attempt = findAttempt(token);
        LocaleResolution resolution = resolveAndPersist(attempt, requestedLocale);
        LocaleBundle bundle = loadBundle(attempt.versionId(), resolution.selectedLocale());
        if (bundle == null) {
            return response;
        }
        return new RegistrarRespostaResponse(
                response.participacaoId(),
                response.status(),
                response.repetida(),
                response.finalizado(),
                response.redirectUrl(),
                response.progresso(),
                localizeStage(response.etapaAtual(), bundle)
        );
    }

    @Transactional
    public CandidateResultPageResponse localizeResult(
            String token,
            String requestedLocale,
            CandidateResultPageResponse response
    ) {
        AttemptLocaleContext attempt = findAttempt(token);
        LocaleResolution resolution = resolveAndPersist(attempt, requestedLocale);
        LocaleBundle bundle = loadBundle(attempt.versionId(), resolution.selectedLocale());
        if (bundle == null) {
            return response;
        }
        List<CandidateResultItemResponse> resultItems = response.resultados().stream()
                .map(item -> {
                    CompetencyText translation = bundle.competencies().get(item.titulo());
                    return translation == null
                            ? item
                            : new CandidateResultItemResponse(translation.displayName(), item.pontuacao(), item.resultado());
                })
                .toList();
        return new CandidateResultPageResponse(
                defaultIfBlank(bundle.title(), response.avaliacaoNome()),
                response.status(),
                response.finalizado(),
                response.redirectUrl(),
                response.concluidoEm(),
                resultItems,
                response.pontuacaoBruta(),
                response.pontuacaoMaximaCaminho(),
                response.pontuacaoNormalizada(),
                response.versaoAlgoritmoPontuacao()
        );
    }

    @Transactional(readOnly = true)
    public List<String> availableLocales(String token) {
        AttemptLocaleContext attempt = findAttempt(token);
        return approvedLocales(attempt.versionId());
    }

    private LocaleResolution resolveAndPersist(AttemptLocaleContext attempt, String requestedLocale) {
        String requested = requestedLocale == null || requestedLocale.isBlank()
                ? attempt.selectedLocale()
                : requestedLocale;
        LocaleResolution resolution = resolveLocale(attempt, requested);
        LocaleSource source;
        if (resolution.fallbackApplied()) {
            source = LocaleSource.BASE_FALLBACK;
        } else if (requestedLocale != null && !requestedLocale.isBlank()) {
            source = LocaleSource.CANDIDATE;
        } else if (attempt.localeSource() != null) {
            source = LocaleSource.valueOf(attempt.localeSource());
        } else {
            source = LocaleSource.BASE_FALLBACK;
        }
        persistSelection(attempt, resolution.selectedLocale(), source, resolution.fallbackApplied());
        return resolution;
    }

    private LocaleResolution resolveLocale(AttemptLocaleContext attempt, String requestedLocale) {
        List<String> available = approvedLocales(attempt.versionId());
        if (available.isEmpty()) {
            return new LocaleResolution(null, List.of(), false);
        }
        String normalizedRequested = requestedLocale == null || requestedLocale.isBlank()
                ? null
                : normalizeLocale(requestedLocale);
        if (normalizedRequested != null && available.contains(normalizedRequested)) {
            return new LocaleResolution(normalizedRequested, available, false);
        }
        String languageOnly = normalizedRequested == null ? null : normalizedRequested.split("-", 2)[0];
        if (languageOnly != null) {
            String compatible = available.stream()
                    .filter(locale -> locale.equals(languageOnly) || locale.startsWith(languageOnly + "-"))
                    .findFirst()
                    .orElse(null);
            if (compatible != null) {
                return new LocaleResolution(compatible, available, false);
            }
        }
        String base = jdbcTemplate.query("""
                SELECT locale FROM simulation_version_locales
                 WHERE simulation_version_id = :versionId AND is_base_locale = TRUE
                 LIMIT 1
                """, new MapSqlParameterSource("versionId", attempt.versionId()),
                resultSet -> resultSet.next() ? resultSet.getString("locale") : null);
        String selected = base != null ? base : available.getFirst();
        return new LocaleResolution(selected, available, normalizedRequested != null && !selected.equals(normalizedRequested));
    }

    private void persistSelection(
            AttemptLocaleContext attempt,
            String selectedLocale,
            LocaleSource source,
            boolean fallbackApplied
    ) {
        if (selectedLocale == null) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE candidate_attempts
                   SET selected_locale = :selectedLocale,
                       locale_source = :source,
                       locale_selected_at = :selectedAt
                 WHERE id = :attemptId AND empresa_id = :empresaId
                """, new MapSqlParameterSource()
                .addValue("selectedLocale", selectedLocale)
                .addValue("source", fallbackApplied ? LocaleSource.BASE_FALLBACK.name() : source.name())
                .addValue("selectedAt", Instant.now())
                .addValue("attemptId", attempt.attemptId())
                .addValue("empresaId", attempt.empresaId()));
    }

    private List<String> approvedLocales(long versionId) {
        return jdbcTemplate.queryForList("""
                SELECT locale
                  FROM simulation_version_locales
                 WHERE simulation_version_id = :versionId
                   AND (status = 'APPROVED' OR is_base_locale = TRUE)
                 ORDER BY is_base_locale DESC, locale
                """, new MapSqlParameterSource("versionId", versionId), String.class);
    }

    private AttemptLocaleContext findAttempt(String token) {
        CandidateAttemptTokenResolver.ResolvedAttemptToken resolved = tokenResolver.resolve(token);
        StringBuilder sql = new StringBuilder("""
                SELECT id, empresa_id, simulation_version_id, selected_locale, locale_source
                  FROM candidate_attempts
                 WHERE id = :attemptId
                """);
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("attemptId", resolved.attemptId());
        if (resolved.empresaId() != null && !resolved.empresaId().isBlank()) {
            sql.append(" AND empresa_id = :empresaId");
            parameters.addValue("empresaId", resolved.empresaId());
        }
        List<AttemptLocaleContext> rows = jdbcTemplate.query(sql.toString(), parameters,
                (resultSet, rowNumber) -> new AttemptLocaleContext(
                        resultSet.getString("id"),
                        resultSet.getString("empresa_id"),
                        resultSet.getLong("simulation_version_id"),
                        resultSet.getString("selected_locale"),
                        resultSet.getString("locale_source")
                ));
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Participação não encontrada para seleção de idioma.");
        }
        return rows.getFirst();
    }

    private LocaleBundle loadBundle(long versionId, String locale) {
        if (locale == null) {
            return null;
        }
        List<LocaleHeader> headers = jdbcTemplate.query("""
                SELECT l.id, c.title, c.description, c.instructions, c.report_introduction
                  FROM simulation_version_locales l
                  JOIN simulation_locale_contents c ON c.locale_id = l.id
                 WHERE l.simulation_version_id = :versionId
                   AND l.locale = :locale
                   AND (l.status = 'APPROVED' OR l.is_base_locale = TRUE)
                """, new MapSqlParameterSource()
                .addValue("versionId", versionId)
                .addValue("locale", locale),
                (resultSet, rowNumber) -> new LocaleHeader(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("title"),
                        resultSet.getString("description"),
                        resultSet.getString("instructions"),
                        resultSet.getString("report_introduction")
                ));
        if (headers.isEmpty()) {
            return null;
        }
        LocaleHeader header = headers.getFirst();
        Map<String, NodeText> nodes = jdbcTemplate.query("""
                SELECT node_id, speaker, message, report_text, plain_text_description, media_transcript
                  FROM simulation_node_translations
                 WHERE locale_id = :localeId
                """, new MapSqlParameterSource("localeId", header.localeId()),
                resultSet -> {
                    Map<String, NodeText> values = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        values.put(resultSet.getString("node_id"), new NodeText(
                                resultSet.getString("speaker"),
                                resultSet.getString("message"),
                                resultSet.getString("report_text"),
                                resultSet.getString("plain_text_description"),
                                resultSet.getString("media_transcript")
                        ));
                    }
                    return values;
                });
        Map<String, OptionText> options = jdbcTemplate.query("""
                SELECT node_id, option_id, text, plain_text_description, media_transcript
                  FROM simulation_option_translations
                 WHERE locale_id = :localeId
                """, new MapSqlParameterSource("localeId", header.localeId()),
                resultSet -> {
                    Map<String, OptionText> values = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        values.put(optionKey(resultSet.getString("node_id"), resultSet.getString("option_id")),
                                new OptionText(
                                        resultSet.getString("text"),
                                        resultSet.getString("plain_text_description"),
                                        resultSet.getString("media_transcript")
                                ));
                    }
                    return values;
                });
        Map<String, CompetencyText> competencies = jdbcTemplate.query("""
                SELECT competency_name, display_name, report_text
                  FROM simulation_competency_translations
                 WHERE locale_id = :localeId
                """, new MapSqlParameterSource("localeId", header.localeId()),
                resultSet -> {
                    Map<String, CompetencyText> values = new LinkedHashMap<>();
                    while (resultSet.next()) {
                        values.put(resultSet.getString("competency_name"),
                                new CompetencyText(
                                        resultSet.getString("display_name"),
                                        resultSet.getString("report_text")
                                ));
                    }
                    return values;
                });
        return new LocaleBundle(
                header.title(),
                header.description(),
                header.instructions(),
                header.reportIntroduction(),
                nodes,
                options,
                competencies
        );
    }

    private EtapaAtualResponse localizeStage(EtapaAtualResponse stage, LocaleBundle bundle) {
        if (stage == null) {
            return null;
        }
        NodeText node = bundle.nodes().get(stage.id());
        List<RespostaResponse> alternatives = stage.alternativas() == null
                ? null
                : stage.alternativas().stream()
                        .map(answer -> localizeAnswer(stage.id(), answer, bundle))
                        .toList();
        if (node == null && alternatives == stage.alternativas()) {
            return stage;
        }
        return new EtapaAtualResponse(
                stage.id(),
                stage.numero(),
                defaultIfBlank(node == null ? null : node.speaker(), stage.pessoa()),
                defaultIfBlank(node == null ? null : node.message(), stage.descricao()),
                defaultIfBlank(node == null ? null : node.plainTextDescription(), stage.descricaoAcessivel()),
                stage.tempoLimiteSegundos(),
                stage.tempoLimiteSegundosAcomodado(),
                stage.audioDescricaoUrl(),
                stage.midiaUrl(),
                stage.tipoMidia(),
                defaultIfBlank(node == null ? null : node.mediaTranscript(), stage.transcricaoMidia()),
                stage.legendaMidiaUrl(),
                stage.versaoMidia(),
                alternatives
        );
    }

    private RespostaResponse localizeAnswer(String nodeId, RespostaResponse answer, LocaleBundle bundle) {
        OptionText translation = bundle.options().get(optionKey(nodeId, answer.id()));
        if (translation == null) {
            return answer;
        }
        return new RespostaResponse(
                answer.id(),
                defaultIfBlank(translation.text(), answer.texto()),
                defaultIfBlank(translation.plainTextDescription(), answer.descricaoAcessivel()),
                answer.audioDescricaoUrl(),
                answer.midiaUrl(),
                answer.tipoMidia(),
                defaultIfBlank(translation.mediaTranscript(), answer.transcricaoMidia()),
                answer.legendaMidiaUrl(),
                answer.versaoMidia()
        );
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String optionKey(String nodeId, String optionId) {
        return nodeId + "\u0000" + optionId;
    }

    private String normalizeLocale(String locale) {
        String[] parts = locale.trim().split("-", 2);
        return parts.length == 1
                ? parts[0].toLowerCase(Locale.ROOT)
                : parts[0].toLowerCase(Locale.ROOT) + "-" + parts[1].toUpperCase(Locale.ROOT);
    }

    private record AttemptLocaleContext(
            String attemptId,
            String empresaId,
            long versionId,
            String selectedLocale,
            String localeSource
    ) {
    }

    private record LocaleResolution(
            String selectedLocale,
            List<String> availableLocales,
            boolean fallbackApplied
    ) {
    }

    private record LocaleHeader(
            UUID localeId,
            String title,
            String description,
            String instructions,
            String reportIntroduction
    ) {
    }

    private record LocaleBundle(
            String title,
            String description,
            String instructions,
            String reportIntroduction,
            Map<String, NodeText> nodes,
            Map<String, OptionText> options,
            Map<String, CompetencyText> competencies
    ) {
    }

    private record NodeText(
            String speaker,
            String message,
            String reportText,
            String plainTextDescription,
            String mediaTranscript
    ) {
    }

    private record OptionText(
            String text,
            String plainTextDescription,
            String mediaTranscript
    ) {
    }

    private record CompetencyText(String displayName, String reportText) {
    }
}
