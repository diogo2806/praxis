package br.com.iforce.praxis.journey.controller;

import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;
import br.com.iforce.praxis.journey.dto.JourneyConsolidatedResultResponse;
import br.com.iforce.praxis.journey.service.AssessmentJourneyAttemptLifecycleService;
import br.com.iforce.praxis.journey.service.AssessmentJourneyAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API pública usada pela tela do candidato para executar uma Jornada de Avaliação.
 */
@RestController
@RequestMapping("/candidate/journey-attempts")
@Tag(name = "Candidate Journey Attempts", description = "Fluxo público do candidato na Jornada de Avaliação.")
public class PublicAssessmentJourneyAttemptController {

    private final AssessmentJourneyAttemptService attemptService;
    private final AssessmentJourneyAttemptLifecycleService lifecycleService;

    public PublicAssessmentJourneyAttemptController(
            AssessmentJourneyAttemptService attemptService,
            AssessmentJourneyAttemptLifecycleService lifecycleService
    ) {
        this.attemptService = attemptService;
        this.lifecycleService = lifecycleService;
    }

    @GetMapping("/{attemptId}")
    @Operation(summary = "Consulta progresso público da jornada")
    public ResponseEntity<AssessmentJourneyAttemptResponse> get(@PathVariable String attemptId) {
        lifecycleService.assertUsable(attemptId);
        return ResponseEntity.ok(attemptService.getAttempt(attemptId));
    }

    @PostMapping("/{attemptId}/start")
    @Operation(summary = "Inicia a jornada pelo fluxo público")
    public ResponseEntity<AssessmentJourneyAttemptResponse> start(@PathVariable String attemptId) {
        lifecycleService.assertUsable(attemptId);
        return ResponseEntity.ok(attemptService.startAttempt(attemptId));
    }

    @PostMapping("/{attemptId}/steps/{stepId}/start")
    @Operation(summary = "Inicia etapa da jornada pelo fluxo público")
    public ResponseEntity<AssessmentJourneyAttemptResponse> startStep(
            @PathVariable String attemptId,
            @PathVariable Long stepId
    ) {
        lifecycleService.assertUsable(attemptId);
        return ResponseEntity.ok(attemptService.startStep(attemptId, stepId));
    }

    @PostMapping("/{attemptId}/steps/{stepId}/complete")
    @Operation(summary = "Conclui etapa da jornada pelo fluxo público")
    public ResponseEntity<AssessmentJourneyAttemptResponse> completeStep(
            @PathVariable String attemptId,
            @PathVariable Long stepId
    ) {
        lifecycleService.assertUsable(attemptId);
        return ResponseEntity.ok(attemptService.completeStep(attemptId, stepId));
    }

    @GetMapping("/{attemptId}/result")
    @Operation(summary = "Resultado consolidado público da jornada")
    public ResponseEntity<JourneyConsolidatedResultResponse> result(@PathVariable String attemptId) {
        lifecycleService.assertUsable(attemptId);
        return ResponseEntity.ok(attemptService.getConsolidatedResult(attemptId));
    }
}
