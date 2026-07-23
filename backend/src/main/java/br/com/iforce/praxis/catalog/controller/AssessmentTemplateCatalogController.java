package br.com.iforce.praxis.catalog.controller;

import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.CreateTemplateRequest;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.InstantiateTemplateRequest;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.InstantiateTemplateResponse;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.ReviewTemplateRequest;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.TemplateResponse;
import br.com.iforce.praxis.catalog.dto.AssessmentTemplateDtos.TemplateSearch;
import br.com.iforce.praxis.catalog.service.AssessmentTemplateCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/assessment-templates")
@Tag(name = "Assessment Template Catalog", description = "Biblioteca governada de modelos de avaliação.")
public class AssessmentTemplateCatalogController {

    private final AssessmentTemplateCatalogService catalogService;

    public AssessmentTemplateCatalogController(AssessmentTemplateCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping
    @Operation(summary = "Pesquisa modelos visíveis por filtros de negócio")
    public ResponseEntity<List<TemplateResponse>> search(
            @Valid @ParameterObject @ModelAttribute TemplateSearch search
    ) {
        return ResponseEntity.ok(catalogService.search(search));
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "Consulta a prévia e a governança de um modelo")
    public ResponseEntity<TemplateResponse> get(@PathVariable UUID templateId) {
        return ResponseEntity.ok(catalogService.get(templateId));
    }

    @PostMapping
    @Operation(summary = "Cadastra uma versão de avaliação como modelo")
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.status(201).body(catalogService.create(request));
    }

    @PostMapping("/{templateId}/submit")
    @Operation(summary = "Envia o modelo para revisão independente")
    public ResponseEntity<TemplateResponse> submit(@PathVariable UUID templateId) {
        return ResponseEntity.ok(catalogService.submitForReview(templateId));
    }

    @PostMapping("/{templateId}/review")
    @Operation(summary = "Aprova ou rejeita um modelo em revisão")
    public ResponseEntity<TemplateResponse> review(
            @PathVariable UUID templateId,
            @Valid @RequestBody ReviewTemplateRequest request
    ) {
        return ResponseEntity.ok(catalogService.review(templateId, request));
    }

    @PostMapping("/{templateId}/favorite")
    @Operation(summary = "Adiciona ou remove o modelo dos favoritos do usuário")
    public ResponseEntity<TemplateResponse> toggleFavorite(@PathVariable UUID templateId) {
        return ResponseEntity.ok(catalogService.toggleFavorite(templateId));
    }

    @PostMapping("/{templateId}/instantiate")
    @Operation(summary = "Cria avaliação independente em rascunho a partir do modelo")
    public ResponseEntity<InstantiateTemplateResponse> instantiate(
            @PathVariable UUID templateId,
            @Valid @RequestBody InstantiateTemplateRequest request
    ) {
        return ResponseEntity.status(201).body(catalogService.instantiate(templateId, request));
    }
}
