package br.com.iforce.praxis.quality.controller;

import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.ExternalCriterionRequest;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.ExternalCriterionResponse;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.QualityReportResponse;
import br.com.iforce.praxis.quality.dto.AssessmentQualityDtos.SensitiveAnalysisRequest;
import br.com.iforce.praxis.quality.service.AssessmentQualityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/assessment-quality")
@Tag(name = "Assessment quality", description = "Qualidade psicométrica, desempenho, critérios externos e análises protegidas.")
@PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','RESULTS_ANALYST')")
public class AssessmentQualityController {

    private final AssessmentQualityService qualityService;

    public AssessmentQualityController(AssessmentQualityService qualityService) {
        this.qualityService = qualityService;
    }

    @GetMapping("/report")
    @Operation(summary = "Calcula métricas observadas e estimadas sem misturar empresas")
    public ResponseEntity<QualityReportResponse> report(
            @RequestParam String simulationId,
            @RequestParam(required = false) Integer versionNumber,
            @RequestParam(required = false) Long gupyJobId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        return ResponseEntity.ok(qualityService.report(simulationId, versionNumber, gupyJobId, from, to));
    }

    @PostMapping("/sensitive-report")
    @Operation(summary = "Executa análise por grupo com finalidade, base legal, supressão e auditoria")
    public ResponseEntity<QualityReportResponse> sensitiveReport(
            @RequestParam String simulationId,
            @RequestParam(required = false) Integer versionNumber,
            @RequestParam(required = false) Long gupyJobId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @Valid @RequestBody SensitiveAnalysisRequest request
    ) {
        return ResponseEntity.ok(qualityService.sensitiveReport(
                simulationId, versionNumber, gupyJobId, from, to, request
        ));
    }

    @PostMapping("/external-criteria")
    @Operation(summary = "Cadastra ou atualiza critério externo por participação")
    public ResponseEntity<ExternalCriterionResponse> saveExternalCriterion(
            @Valid @RequestBody ExternalCriterionRequest request
    ) {
        return ResponseEntity.ok(qualityService.saveExternalCriterion(request));
    }

    @GetMapping("/external-criteria")
    @Operation(summary = "Lista critérios externos da avaliação na empresa atual")
    public ResponseEntity<List<ExternalCriterionResponse>> listExternalCriteria(
            @RequestParam(required = false) String simulationId,
            @RequestParam(required = false) Integer versionNumber
    ) {
        return ResponseEntity.ok(qualityService.listExternalCriteria(simulationId, versionNumber));
    }

    @GetMapping(value = "/technical-report.csv", produces = "text/csv")
    @Operation(summary = "Exporta relatório técnico com metodologia, limitações e natureza da evidência")
    public ResponseEntity<byte[]> exportTechnicalReport(
            @RequestParam String simulationId,
            @RequestParam(required = false) Integer versionNumber,
            @RequestParam(required = false) Long gupyJobId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        byte[] content = qualityService.exportTechnicalCsv(simulationId, versionNumber, gupyJobId, from, to);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("qualidade-avaliacao-" + simulationId + ".csv", StandardCharsets.UTF_8)
                .build());
        return ResponseEntity.ok().headers(headers).body(content);
    }
}
