package br.com.iforce.praxis.journey.controller;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.candidate.dto.ExtendCandidateLinkRequest;
import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;
import br.com.iforce.praxis.journey.dto.CreateJourneyAttemptRequest;
import br.com.iforce.praxis.journey.dto.JourneyConsolidatedResultResponse;
import br.com.iforce.praxis.journey.service.AssessmentJourneyAttemptLifecycleService;
import br.com.iforce.praxis.journey.service.AssessmentJourneyAttemptService;
import br.com.iforce.praxis.journey.service.AssessmentJourneyInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Porta de entrada (API) da execução das Jornadas de Avaliação pelo candidato.
 */
@RestController
@RequestMapping("/api/v1/assessment-journey-attempts")
@Tag(name = "Assessment Journey Attempts", description = "Execução, convite e progresso de Jornadas de Avaliação.")
public class AssessmentJourneyAttemptController {

    private final AssessmentJourneyAttemptService attemptService;
    private final AssessmentJourneyAttemptLifecycleService lifecycleService;
    private final AssessmentJourneyInvitationService invitationService;
    private final AuditEventService auditEventService;

    public AssessmentJourneyAttemptController(
            AssessmentJourneyAttemptService attemptService,
            AssessmentJourneyAttemptLifecycleService lifecycleService,
            AssessmentJourneyInvitationService invitationService,
            AuditEventService auditEventService
    ) {
        this.attemptService = attemptService;
        this.lifecycleService = lifecycleService;
        this.invitationService = invitationService;
        this.auditEventService = auditEventService;
    }

    @PostMapping
    @Operation(summary = "Cria tentativa da jornada e envia o convite ao candidato")
    public ResponseEntity<AssessmentJourneyAttemptResponse> create(
            @Valid @RequestBody CreateJourneyAttemptRequest request
    ) {
        AssessmentJourneyAttemptResponse response = attemptService.createAttempt(request);
        invitationService.sendInvitation(response);
        lifecycleService.markInvitationSent(response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{attemptId}")
    @Operation(summary = "Consulta progresso da jornada")
    public ResponseEntity<AssessmentJourneyAttemptResponse> get(@PathVariable String attemptId) {
        lifecycleService.refreshStatus(attemptId);
        return ResponseEntity.ok(attemptService.getAttempt(attemptId));
    }

    @PostMapping("/{attemptId}/start")
    @Operation(summary = "Inicia a jornada")
    public ResponseEntity<AssessmentJourneyAttemptResponse> start(@PathVariable String attemptId) {
        lifecycleService.assertUsable(attemptId);
        return ResponseEntity.ok(attemptService.startAttempt(attemptId));
    }

    @PostMapping("/{attemptId}/steps/{stepId}/start")
    @Operation(summary = "Inicia etapa/teste da jornada")
    public ResponseEntity<AssessmentJourneyAttemptResponse> startStep(
            @PathVariable String attemptId,
            @PathVariable Long stepId
    ) {
        lifecycleService.assertUsable(attemptId);
        return ResponseEntity.ok(attemptService.startStep(attemptId, stepId));
    }

    @PostMapping("/{attemptId}/steps/{stepId}/complete")
    @Operation(summary = "Conclui etapa/teste da jornada")
    public ResponseEntity<AssessmentJourneyAttemptResponse> completeStep(
            @PathVariable String attemptId,
            @PathVariable Long stepId
    ) {
        return ResponseEntity.ok(attemptService.completeStep(attemptId, stepId));
    }

    @PostMapping("/{attemptId}/resend")
    @Operation(summary = "Reenvia o convite vigente da jornada")
    public ResponseEntity<Void> resend(@PathVariable String attemptId) {
        lifecycleService.resendInvitation(attemptId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{attemptId}/extend")
    @Operation(summary = "Amplia ou reativa a validade do convite da jornada")
    public ResponseEntity<Void> extend(
            @PathVariable String attemptId,
            @Valid @RequestBody ExtendCandidateLinkRequest request
    ) {
        lifecycleService.extendValidity(attemptId, request.additionalDays());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{attemptId}/cancel")
    @Operation(summary = "Cancela a participação da jornada")
    public ResponseEntity<Void> cancel(@PathVariable String attemptId) {
        lifecycleService.cancel(attemptId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{attemptId}/result")
    @Operation(summary = "Resultado consolidado da jornada")
    public ResponseEntity<JourneyConsolidatedResultResponse> result(@PathVariable String attemptId) {
        return ResponseEntity.ok(attemptService.getConsolidatedResult(attemptId));
    }

    @GetMapping("/{attemptId}/audit-events")
    @Operation(summary = "Trilha de auditoria da tentativa da jornada")
    public ResponseEntity<List<AuditEventResponse>> auditEvents(@PathVariable String attemptId) {
        attemptService.getAttempt(attemptId);
        return ResponseEntity.ok(auditEventService.listAssessmentJourneyAttemptEvents(attemptId));
    }
}
