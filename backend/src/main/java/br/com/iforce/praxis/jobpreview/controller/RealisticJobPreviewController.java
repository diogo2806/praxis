package br.com.iforce.praxis.jobpreview.controller;

import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.CandidatePreviewReactionRequest;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.CandidatePreviewResponse;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.CreatePreviewRequest;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.DisplayStage;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.PreviewMetricsResponse;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.PreviewSummaryResponse;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.PreviewVersionResponse;
import br.com.iforce.praxis.jobpreview.dto.RealisticJobPreviewDtos.UpdatePreviewDraftRequest;
import br.com.iforce.praxis.jobpreview.service.RealisticJobPreviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Realistic job preview", description = "Prévia realista versionada, acessível e separada da pontuação.")
public class RealisticJobPreviewController {

    private final RealisticJobPreviewService previewService;

    public RealisticJobPreviewController(RealisticJobPreviewService previewService) {
        this.previewService = previewService;
    }

    @PostMapping("/api/v1/realistic-job-previews")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR')")
    @Operation(summary = "Cria uma prévia realista com rascunho inicial")
    public ResponseEntity<PreviewVersionResponse> create(@Valid @RequestBody CreatePreviewRequest request) {
        return ResponseEntity.ok(previewService.create(request));
    }

    @GetMapping("/api/v1/realistic-job-previews")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR','RESULTS_ANALYST')")
    @Operation(summary = "Lista prévias realistas da empresa atual")
    public ResponseEntity<List<PreviewSummaryResponse>> list() {
        return ResponseEntity.ok(previewService.list());
    }

    @GetMapping("/api/v1/realistic-job-previews/{previewId}/draft")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR')")
    public ResponseEntity<PreviewVersionResponse> getDraft(@PathVariable String previewId) {
        return ResponseEntity.ok(previewService.getDraft(previewId));
    }

    @PutMapping("/api/v1/realistic-job-previews/{previewId}/draft")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR')")
    @Operation(summary = "Atualiza apenas o rascunho, preservando versões publicadas")
    public ResponseEntity<PreviewVersionResponse> updateDraft(
            @PathVariable String previewId,
            @Valid @RequestBody UpdatePreviewDraftRequest request
    ) {
        return ResponseEntity.ok(previewService.updateDraft(previewId, request));
    }

    @PostMapping("/api/v1/realistic-job-previews/{previewId}/publish")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER')")
    @Operation(summary = "Publica o rascunho e cria a próxima versão editável")
    public ResponseEntity<PreviewVersionResponse> publish(@PathVariable String previewId) {
        return ResponseEntity.ok(previewService.publish(previewId));
    }

    @GetMapping("/api/v1/realistic-job-previews/{previewId}/metrics")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','RESULTS_ANALYST')")
    @Operation(summary = "Retorna métricas agregadas com supressão de amostra pequena")
    public ResponseEntity<PreviewMetricsResponse> metrics(
            @PathVariable String previewId,
            @RequestParam(required = false) Integer versionNumber
    ) {
        return ResponseEntity.ok(previewService.metrics(previewId, versionNumber));
    }

    @PostMapping("/api/v1/candidate/{token}/realistic-preview/present")
    @Operation(summary = "Apresenta ao candidato a versão publicada aplicável à tentativa")
    public ResponseEntity<CandidatePreviewResponse> present(
            @PathVariable String token,
            @RequestParam DisplayStage stage
    ) {
        return ResponseEntity.ok(previewService.present(token, stage));
    }

    @PostMapping("/api/v1/candidate/{token}/realistic-preview/{versionId}/reaction")
    @Operation(summary = "Registra ciência, desistência voluntária e reação opcional sem alterar a nota")
    public ResponseEntity<Void> react(
            @PathVariable String token,
            @PathVariable String versionId,
            @RequestParam DisplayStage stage,
            @Valid @RequestBody CandidatePreviewReactionRequest request
    ) {
        previewService.react(token, versionId, stage, request);
        return ResponseEntity.noContent().build();
    }
}
