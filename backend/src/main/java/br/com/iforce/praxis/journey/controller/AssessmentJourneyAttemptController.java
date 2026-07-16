package br.com.iforce.praxis.journey.controller;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;

import br.com.iforce.praxis.journey.dto.CreateJourneyAttemptRequest;

import br.com.iforce.praxis.journey.dto.JourneyConsolidatedResultResponse;

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
 *
 * <p>É por aqui que a empresa gera a tentativa de jornada de um candidato,
 * acompanha o progresso, libera cada teste na ordem definida e consulta o
 * resultado consolidado. A orquestração reaproveita as tentativas individuais
 * de cada teste, preservando o histórico individual.</p>
 */
@RestController
@RequestMapping("/api/v1/assessment-journey-attempts")
@Tag(name = "Assessment Journey Attempts", description = "Execução e progresso de Jornadas de Avaliação pelo candidato.")
public class AssessmentJourneyAttemptController {

    private final AssessmentJourneyAttemptService attemptService;
    private final AssessmentJourneyInvitationService invitationService;
    private final AuditEventService auditEventService;

    public AssessmentJourneyAttemptController(
            AssessmentJourneyAttemptService attemptService,
            AssessmentJourneyInvitationService invitationService,
            AuditEventService auditEventService
    ) {
        this.attemptService = attemptService;
        this.invitationService = invitationService;
        this.auditEventService = auditEventService;
    }

    /**
     * Cria a tentativa de uma jornada para um candidato e envia o convite por e-mail.
     *
     * @param request jornada, dados do candidato e sequência
     * @return o progresso inicial da tentativa
     */
    @PostMapping
    @Operation(summary = "Cria tentativa da jornada e envia o convite ao candidato")
    public ResponseEntity<AssessmentJourneyAttemptResponse> create(
            @Valid @RequestBody CreateJourneyAttemptRequest request
    ) {
        AssessmentJourneyAttemptResponse response = attemptService.createAttempt(request);
        invitationService.sendInvitation(response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Consulta o progresso da tentativa da jornada.
     *
     * @param attemptId identificador da tentativa
     * @return o progresso atual
     */
    @GetMapping("/{attemptId}")
    @Operation(summary = "Consulta progresso da jornada")
    public ResponseEntity<AssessmentJourneyAttemptResponse> get(@PathVariable String attemptId) {
        return ResponseEntity.ok(attemptService.getAttempt(attemptId));
    }

    /**
     * Inicia a jornada.
     *
     * @param attemptId identificador da tentativa
     * @return o progresso atualizado
     */
    @PostMapping("/{attemptId}/start")
    @Operation(summary = "Inicia a jornada")
    public ResponseEntity<AssessmentJourneyAttemptResponse> start(@PathVariable String attemptId) {
        return ResponseEntity.ok(attemptService.startAttempt(attemptId));
    }

    /**
     * Inicia uma etapa/teste da jornada, gerando a tentativa individual do teste.
     *
     * @param attemptId identificador da tentativa
     * @param stepId identificador da etapa
     * @return o progresso atualizado, com o link do candidato para o teste
     */
    @PostMapping("/{attemptId}/steps/{stepId}/start")
    @Operation(summary = "Inicia etapa/teste da jornada")
    public ResponseEntity<AssessmentJourneyAttemptResponse> startStep(
            @PathVariable String attemptId,
            @PathVariable Long stepId
    ) {
        return ResponseEntity.ok(attemptService.startStep(attemptId, stepId));
    }

    /**
     * Marca a etapa como concluída após a conclusão da tentativa individual.
     *
     * @param attemptId identificador da tentativa
     * @param stepId identificador da etapa
     * @return o progresso atualizado
     */
    @PostMapping("/{attemptId}/steps/{stepId}/complete")
    @Operation(summary = "Conclui etapa/teste da jornada")
    public ResponseEntity<AssessmentJourneyAttemptResponse> completeStep(
            @PathVariable String attemptId,
            @PathVariable Long stepId
    ) {
        return ResponseEntity.ok(attemptService.completeStep(attemptId, stepId));
    }

    /**
     * Recupera o resultado consolidado do candidato na jornada.
     *
     * @param attemptId identificador da tentativa
     * @return o resultado consolidado (separado por teste)
     */
    @GetMapping("/{attemptId}/result")
    @Operation(summary = "Resultado consolidado da jornada")
    public ResponseEntity<JourneyConsolidatedResultResponse> result(@PathVariable String attemptId) {
        return ResponseEntity.ok(attemptService.getConsolidatedResult(attemptId));
    }

    /**
     * Recupera a trilha de auditoria da tentativa da jornada.
     *
     * @param attemptId identificador da tentativa
     * @return os eventos de auditoria da tentativa
     */
    @GetMapping("/{attemptId}/audit-events")
    @Operation(summary = "Trilha de auditoria da tentativa da jornada")
    public ResponseEntity<List<AuditEventResponse>> auditEvents(@PathVariable String attemptId) {
        // Garante que a tentativa pertence ao empresa antes de expor sua trilha.
        attemptService.getAttempt(attemptId);
        return ResponseEntity.ok(auditEventService.listAssessmentJourneyAttemptEvents(attemptId));
    }
}
