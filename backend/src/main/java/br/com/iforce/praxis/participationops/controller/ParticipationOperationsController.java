package br.com.iforce.praxis.participationops.controller;

import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkJobResponse;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkPreviewRequest;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkPreviewResponse;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkRequest;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.ParticipationRef;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.ParticipationTagsResponse;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.SavedViewRequest;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.SavedViewResponse;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.TagRequest;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.TagResponse;
import br.com.iforce.praxis.participationops.service.ParticipationBulkService;
import br.com.iforce.praxis.participationops.service.ParticipationSavedViewService;
import br.com.iforce.praxis.participationops.service.ParticipationTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/participation-operations")
@Tag(name = "Participation operations", description = "Visões salvas, tags e operações auditáveis em lote.")
@PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','OPERATIONS_MANAGER')")
public class ParticipationOperationsController {

    private final ParticipationSavedViewService savedViewService;
    private final ParticipationTagService tagService;
    private final ParticipationBulkService bulkService;
    private final CurrentUserService currentUserService;

    public ParticipationOperationsController(
            ParticipationSavedViewService savedViewService,
            ParticipationTagService tagService,
            ParticipationBulkService bulkService,
            CurrentUserService currentUserService
    ) {
        this.savedViewService = savedViewService;
        this.tagService = tagService;
        this.bulkService = bulkService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/views")
    @Operation(summary = "Lista visões próprias e compartilhadas da empresa")
    public ResponseEntity<List<SavedViewResponse>> views() {
        return ResponseEntity.ok(savedViewService.list(currentUserService.requiredUserId()));
    }

    @PostMapping("/views")
    @Operation(summary = "Salva uma visão operacional")
    public ResponseEntity<SavedViewResponse> createView(@Valid @RequestBody SavedViewRequest request) {
        return ResponseEntity.ok(savedViewService.create(currentUserService.requiredUserId(), request));
    }

    @PutMapping("/views/{viewId}")
    @Operation(summary = "Atualiza uma visão do proprietário")
    public ResponseEntity<SavedViewResponse> updateView(
            @PathVariable String viewId,
            @Valid @RequestBody SavedViewRequest request
    ) {
        return ResponseEntity.ok(savedViewService.update(currentUserService.requiredUserId(), viewId, request));
    }

    @DeleteMapping("/views/{viewId}")
    @Operation(summary = "Exclui uma visão do proprietário")
    public ResponseEntity<Void> deleteView(@PathVariable String viewId) {
        savedViewService.delete(currentUserService.requiredUserId(), viewId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tags")
    @Operation(summary = "Lista tags internas da empresa")
    public ResponseEntity<List<TagResponse>> tags() {
        return ResponseEntity.ok(tagService.list());
    }

    @PostMapping("/tags")
    @Operation(summary = "Cria uma tag interna")
    public ResponseEntity<TagResponse> createTag(@Valid @RequestBody TagRequest request) {
        return ResponseEntity.ok(tagService.create(currentUserService.requiredUserId(), request));
    }

    @PutMapping("/tags/{tagId}")
    @Operation(summary = "Atualiza uma tag interna")
    public ResponseEntity<TagResponse> updateTag(
            @PathVariable String tagId,
            @Valid @RequestBody TagRequest request
    ) {
        return ResponseEntity.ok(tagService.update(tagId, request));
    }

    @DeleteMapping("/tags/{tagId}")
    @Operation(summary = "Exclui uma tag e suas atribuições")
    public ResponseEntity<Void> deleteTag(@PathVariable String tagId) {
        tagService.delete(tagId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/tags/query")
    @Operation(summary = "Consulta tags atribuídas a participações")
    public ResponseEntity<List<ParticipationTagsResponse>> assignedTags(
            @Valid @RequestBody List<@Valid ParticipationRef> participations
    ) {
        return ResponseEntity.ok(tagService.tagsFor(participations));
    }

    @PostMapping("/bulk/preview")
    @Operation(summary = "Estima impacto e exceções antes da confirmação")
    public ResponseEntity<BulkPreviewResponse> preview(@Valid @RequestBody BulkPreviewRequest request) {
        return ResponseEntity.ok(bulkService.preview(request));
    }

    @PostMapping("/bulk/jobs")
    @Operation(summary = "Cria operação idempotente e assíncrona em lote")
    public ResponseEntity<BulkJobResponse> createJob(@Valid @RequestBody BulkRequest request) {
        return ResponseEntity.ok(bulkService.create(currentUserService.requiredUserId(), request));
    }

    @GetMapping("/bulk/jobs")
    @Operation(summary = "Lista os lotes recentes da empresa")
    public ResponseEntity<List<BulkJobResponse>> jobs() {
        return ResponseEntity.ok(bulkService.list());
    }

    @GetMapping("/bulk/jobs/{jobId}")
    @Operation(summary = "Consulta progresso e resultado item a item")
    public ResponseEntity<BulkJobResponse> job(@PathVariable String jobId) {
        return ResponseEntity.ok(bulkService.get(jobId));
    }

    @GetMapping("/bulk/jobs/{jobId}/report.csv")
    @Operation(summary = "Baixa relatório minimizado do lote")
    public ResponseEntity<byte[]> report(@PathVariable String jobId) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=participation-bulk-" + jobId + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(bulkService.report(jobId));
    }
}
