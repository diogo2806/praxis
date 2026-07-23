package br.com.iforce.praxis.catalog.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.CreateTemplateRequest;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.InstantiateTemplateRequest;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.InstantiateTemplateResponse;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.ReviewTemplateRequest;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.TemplatePreviewResponse;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.TemplateResponse;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.TemplateScope;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.TemplateSearch;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.TemplateStatus;
import br.com.iforce.praxis.simulation.dto.SimulationVersionDetailResponse;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationCompetencyEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import br.com.iforce.praxis.simulation.service.SimulationDuplicateService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AssessmentTemplateCatalogService {

    private final JdbcTemplate jdbcTemplate;
    private final SimulationVersionRepository simulationVersionRepository;
    private final SimulationDuplicateService simulationDuplicateService;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;

    public AssessmentTemplateCatalogService(
            JdbcTemplate jdbcTemplate,
            SimulationVersionRepository simulationVersionRepository,
            SimulationDuplicateService simulationDuplicateService,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.simulationVersionRepository = simulationVersionRepository;
        this.simulationDuplicateService = simulationDuplicateService;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public TemplateResponse create(CreateTemplateRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String author = currentUserService.requiredUserId();
        if (request.scope() != TemplateScope.INTERNAL && !isAdmin()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Somente administradores podem propor modelos oficiais ou compartilhados."
            );
        }
        SimulationVersionEntity source = loadSource(
                empresaId,
                request.sourceSimulationId(),
                request.sourceVersionNumber()
        );
        validateCompetencies(source, request.competencies());
        Integer templateVersion = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(MAX(template_version), 0) + 1
                        FROM assessment_templates
                        WHERE owner_empresa_id = ? AND source_simulation_id = ? AND source_version_number = ?
                        """,
                Integer.class,
                empresaId,
                request.sourceSimulationId(),
                request.sourceVersionNumber()
        );
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO assessment_templates (
                            id, owner_empresa_id, source_empresa_id, source_simulation_id,
                            source_version_number, template_version, scope, status, title, summary,
                            job_role, business_area, seniority, sector, duration_minutes, language_code,
                            complexity, methodology_evidence, usage_limitations, author_user_id
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id,
                empresaId,
                empresaId,
                request.sourceSimulationId().trim(),
                request.sourceVersionNumber(),
                templateVersion,
                request.scope().name(),
                request.title().trim(),
                request.summary().trim(),
                request.jobRole().trim(),
                request.businessArea().trim(),
                request.seniority().trim(),
                request.sector().trim(),
                request.durationMinutes(),
                request.languageCode().trim(),
                request.complexity().trim(),
                request.methodologyEvidence().trim(),
                request.usageLimitations().trim(),
                author
        );
        insertCompetencies(id, request.competencies());
        return get(id);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> search(TemplateSearch search) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String userId = currentUserService.requiredUserId();
        List<TemplateRow> rows = jdbcTemplate.query(
                """
                        SELECT template.*,
                               EXISTS (
                                   SELECT 1 FROM assessment_template_favorites favorite
                                   WHERE favorite.template_id = template.id AND favorite.user_id = ?
                               ) AS favorite
                        FROM assessment_templates template
                        WHERE template.owner_empresa_id = ?
                           OR (template.status = 'APPROVED' AND template.scope IN ('SHARED', 'OFFICIAL'))
                        ORDER BY favorite DESC, template.published_at DESC NULLS LAST, template.title
                        """,
                (resultSet, rowNum) -> mapRow(resultSet),
                userId,
                empresaId
        );
        return rows.stream()
                .filter(row -> matches(row, search))
                .filter(row -> !Boolean.TRUE.equals(search.favoriteOnly()) || row.favorite())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse get(UUID templateId) {
        return toResponse(findVisible(templateId));
    }

    @Transactional
    public TemplateResponse submitForReview(UUID templateId) {
        TemplateRow template = findOwned(templateId);
        if (template.status() != TemplateStatus.DRAFT && template.status() != TemplateStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente modelos em rascunho ou rejeitados podem ser enviados para revisão.");
        }
        SimulationVersionEntity source = loadSource(
                template.sourceEmpresaId(),
                template.sourceSimulationId(),
                template.sourceVersionNumber()
        );
        if (source.getStatus() != SimulationVersionStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Publique e aprove a versão de origem antes de disponibilizar o modelo no catálogo."
            );
        }
        jdbcTemplate.update(
                """
                        UPDATE assessment_templates
                        SET status = 'IN_REVIEW', reviewed_by = NULL, review_note = NULL,
                            reviewed_at = NULL, published_at = NULL
                        WHERE id = ?
                        """,
                templateId
        );
        return get(templateId);
    }

    @Transactional
    public TemplateResponse review(UUID templateId, ReviewTemplateRequest request) {
        TemplateRow template = findOwned(templateId);
        if (request.decision() != TemplateStatus.APPROVED && request.decision() != TemplateStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A revisão aceita somente APPROVED ou REJECTED.");
        }
        if (template.status() != TemplateStatus.IN_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O modelo não está aguardando revisão.");
        }
        String reviewer = currentUserService.requiredUserId();
        if (reviewer.equals(template.authorUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O autor não pode aprovar o próprio modelo.");
        }
        if (template.scope() != TemplateScope.INTERNAL && !isAdmin()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A aprovação de modelos compartilhados ou oficiais exige perfil ADMIN."
            );
        }
        Instant now = Instant.now();
        jdbcTemplate.update(
                """
                        UPDATE assessment_templates
                        SET status = ?, reviewed_by = ?, review_note = ?, reviewed_at = ?,
                            published_at = CASE WHEN ? = 'APPROVED' THEN ? ELSE NULL END
                        WHERE id = ?
                        """,
                request.decision().name(),
                reviewer,
                trimToNull(request.reviewNote()),
                now,
                request.decision().name(),
                now,
                templateId
        );
        return get(templateId);
    }

    @Transactional
    public TemplateResponse toggleFavorite(UUID templateId) {
        TemplateRow template = findVisible(templateId);
        String userId = currentUserService.requiredUserId();
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assessment_template_favorites WHERE template_id = ? AND user_id = ?",
                Integer.class,
                template.id(),
                userId
        );
        if (existing != null && existing > 0) {
            jdbcTemplate.update(
                    "DELETE FROM assessment_template_favorites WHERE template_id = ? AND user_id = ?",
                    template.id(),
                    userId
            );
        } else {
            jdbcTemplate.update(
                    "INSERT INTO assessment_template_favorites (template_id, user_id) VALUES (?, ?)",
                    template.id(),
                    userId
            );
        }
        return get(templateId);
    }

    @Transactional
    public InstantiateTemplateResponse instantiate(UUID templateId, InstantiateTemplateRequest request) {
        TemplateRow template = findVisible(templateId);
        if (template.status() != TemplateStatus.APPROVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente modelos aprovados podem gerar avaliações.");
        }
        SimulationVersionDetailResponse copy = simulationDuplicateService.duplicateFromCatalog(
                template.sourceEmpresaId(),
                template.sourceSimulationId(),
                template.sourceVersionNumber(),
                template.id().toString(),
                Integer.toString(template.templateVersion()),
                request.newAssessmentName()
        );
        return new InstantiateTemplateResponse(
                template.id(),
                template.templateVersion(),
                copy.simulationId(),
                copy.versionNumber(),
                copy.status().name(),
                template.sourceSimulationId(),
                template.sourceVersionNumber()
        );
    }

    private TemplateRow findVisible(UUID templateId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String userId = currentUserService.requiredUserId();
        List<TemplateRow> rows = jdbcTemplate.query(
                """
                        SELECT template.*,
                               EXISTS (
                                   SELECT 1 FROM assessment_template_favorites favorite
                                   WHERE favorite.template_id = template.id AND favorite.user_id = ?
                               ) AS favorite
                        FROM assessment_templates template
                        WHERE template.id = ?
                          AND (
                              template.owner_empresa_id = ?
                              OR (template.status = 'APPROVED' AND template.scope IN ('SHARED', 'OFFICIAL'))
                          )
                        """,
                (resultSet, rowNum) -> mapRow(resultSet),
                userId,
                templateId,
                empresaId
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo não encontrado ou não disponível para esta empresa.");
        }
        return rows.getFirst();
    }

    private TemplateRow findOwned(UUID templateId) {
        TemplateRow template = findVisible(templateId);
        if (!template.ownerEmpresaId().equals(currentEmpresaService.requiredEmpresaId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente a empresa proprietária pode alterar o modelo.");
        }
        return template;
    }

    private TemplateRow mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new TemplateRow(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("owner_empresa_id"),
                resultSet.getString("source_empresa_id"),
                resultSet.getString("source_simulation_id"),
                resultSet.getInt("source_version_number"),
                resultSet.getInt("template_version"),
                TemplateScope.valueOf(resultSet.getString("scope")),
                TemplateStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("title"),
                resultSet.getString("summary"),
                resultSet.getString("job_role"),
                resultSet.getString("business_area"),
                resultSet.getString("seniority"),
                resultSet.getString("sector"),
                resultSet.getInt("duration_minutes"),
                resultSet.getString("language_code"),
                resultSet.getString("complexity"),
                resultSet.getString("methodology_evidence"),
                resultSet.getString("usage_limitations"),
                resultSet.getString("author_user_id"),
                resultSet.getString("reviewed_by"),
                resultSet.getString("review_note"),
                timestamp(resultSet, "reviewed_at"),
                timestamp(resultSet, "published_at"),
                resultSet.getBoolean("favorite")
        );
    }

    private TemplateResponse toResponse(TemplateRow row) {
        SimulationVersionEntity source = loadSource(
                row.sourceEmpresaId(),
                row.sourceSimulationId(),
                row.sourceVersionNumber()
        );
        return new TemplateResponse(
                row.id(),
                row.ownerEmpresaId(),
                row.sourceEmpresaId(),
                row.sourceSimulationId(),
                row.sourceVersionNumber(),
                row.templateVersion(),
                row.scope(),
                row.status(),
                row.title(),
                row.summary(),
                row.jobRole(),
                row.businessArea(),
                row.seniority(),
                row.sector(),
                row.durationMinutes(),
                row.languageCode(),
                row.complexity(),
                row.methodologyEvidence(),
                row.usageLimitations(),
                row.authorUserId(),
                row.reviewedBy(),
                row.reviewNote(),
                row.reviewedAt(),
                row.publishedAt(),
                row.favorite(),
                preview(source, row.durationMinutes())
        );
    }

    private TemplatePreviewResponse preview(SimulationVersionEntity version, int durationMinutes) {
        int scenarioCount = 0;
        int terminalCount = 0;
        int optionCount = 0;
        Set<String> accessibility = new LinkedHashSet<>();
        for (SimulationNodeEntity node : version.getNodes()) {
            if (node.isFinal()) terminalCount++; else scenarioCount++;
            optionCount += node.getOptions().size();
            collectAccessibility(
                    accessibility,
                    node.getPlainTextDescription(),
                    node.getAudioDescriptionUrl(),
                    node.getMediaTranscript(),
                    node.getMediaCaptionsUrl()
            );
            for (SimulationOptionEntity option : node.getOptions()) {
                collectAccessibility(
                        accessibility,
                        option.getPlainTextDescription(),
                        option.getAudioDescriptionUrl(),
                        option.getMediaTranscript(),
                        option.getMediaCaptionsUrl()
                );
            }
        }
        List<String> competencies = version.getCompetencies().stream()
                .sorted(Comparator.comparing(SimulationCompetencyEntity::getName))
                .map(SimulationCompetencyEntity::getName)
                .toList();
        return new TemplatePreviewResponse(
                scenarioCount,
                terminalCount,
                optionCount,
                durationMinutes,
                competencies,
                List.copyOf(accessibility),
                version.getRootNodeId()
        );
    }

    private void collectAccessibility(
            Set<String> requirements,
            String plainText,
            String audioDescription,
            String transcript,
            String captions
    ) {
        if (plainText != null && !plainText.isBlank()) requirements.add("Descrição em texto simples");
        if (audioDescription != null && !audioDescription.isBlank()) requirements.add("Audiodescrição");
        if (transcript != null && !transcript.isBlank()) requirements.add("Transcrição");
        if (captions != null && !captions.isBlank()) requirements.add("Legendas");
    }

    private SimulationVersionEntity loadSource(String empresaId, String simulationId, int versionNumber) {
        return simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                        empresaId,
                        simulationId,
                        versionNumber
                )
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão de origem do modelo não encontrada."));
    }

    private void validateCompetencies(SimulationVersionEntity source, List<String> requested) {
        Set<String> sourceNames = source.getCompetencies().stream()
                .map(competency -> normalize(competency.getName()))
                .collect(java.util.stream.Collectors.toSet());
        List<String> invalid = requested.stream()
                .filter(name -> !sourceNames.contains(normalize(name)))
                .toList();
        if (!invalid.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Competências não presentes na versão de origem: " + String.join(", ", invalid)
            );
        }
    }

    private void insertCompetencies(UUID templateId, List<String> competencies) {
        competencies.stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .forEach(name -> jdbcTemplate.update(
                        "INSERT INTO assessment_template_competencies (template_id, competency_name) VALUES (?, ?)",
                        templateId,
                        name
                ));
    }

    private boolean matches(TemplateRow row, TemplateSearch search) {
        if (search == null) return true;
        List<String> competencies = loadTemplateCompetencies(row.id());
        return containsAny(
                search.query(),
                row.title(),
                row.summary(),
                row.jobRole(),
                row.businessArea(),
                row.sector(),
                String.join(" ", competencies)
        ) && contains(row.jobRole(), search.jobRole())
                && contains(row.businessArea(), search.businessArea())
                && contains(row.seniority(), search.seniority())
                && contains(row.sector(), search.sector())
                && competencies.stream().anyMatch(value -> contains(value, search.competency()))
                && contains(row.languageCode(), search.languageCode())
                && contains(row.complexity(), search.complexity());
    }

    private boolean containsAny(String filter, String... values) {
        if (filter == null || filter.isBlank()) return true;
        String normalizedFilter = normalize(filter);
        for (String value : values) {
            if (normalize(value).contains(normalizedFilter)) return true;
        }
        return false;
    }

    private boolean contains(String value, String filter) {
        return filter == null || filter.isBlank() || normalize(value).contains(normalize(filter));
    }

    private List<String> loadTemplateCompetencies(UUID templateId) {
        return jdbcTemplate.query(
                "SELECT competency_name FROM assessment_template_competencies WHERE template_id = ? ORDER BY competency_name",
                (resultSet, rowNum) -> resultSet.getString("competency_name"),
                templateId
        );
    }

    private String normalize(String value) {
        return value == null ? "" : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private Instant timestamp(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        java.sql.Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private record TemplateRow(
            UUID id,
            String ownerEmpresaId,
            String sourceEmpresaId,
            String sourceSimulationId,
            int sourceVersionNumber,
            int templateVersion,
            TemplateScope scope,
            TemplateStatus status,
            String title,
            String summary,
            String jobRole,
            String businessArea,
            String seniority,
            String sector,
            int durationMinutes,
            String languageCode,
            String complexity,
            String methodologyEvidence,
            String usageLimitations,
            String authorUserId,
            String reviewedBy,
            String reviewNote,
            Instant reviewedAt,
            Instant publishedAt,
            boolean favorite
    ) {
    }
}
