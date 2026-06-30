package br.com.iforce.praxis.journey.controller;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.journey.dto.AddJourneyStepRequest;
import br.com.iforce.praxis.journey.dto.AssessmentJourneyAttemptResponse;
import br.com.iforce.praxis.journey.dto.AssessmentJourneyDetailResponse;
import br.com.iforce.praxis.journey.dto.AssessmentJourneySummaryResponse;
import br.com.iforce.praxis.journey.dto.CreateAssessmentJourneyRequest;
import br.com.iforce.praxis.journey.dto.UpdateAssessmentJourneyRequest;
import br.com.iforce.praxis.journey.dto.UpdateJourneyStepRequest;
import br.com.iforce.praxis.journey.service.AssessmentJourneyAttemptService;
import br.com.iforce.praxis.journey.service.AssessmentJourneyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Porta de entrada (API) da autoria das Jornadas de Avaliação.
 *
 * <p>É por aqui que a empresa monta processos seletivos maiores reaproveitando
 * simulações publicadas: cria a jornada, organiza testes em sequências, define
 * ordem e obrigatoriedade, publica e arquiva. A jornada é uma camada de
 * orquestração acima das simulações — não substitui o teste individual.</p>
 */
@RestController
@RequestMapping("/api/v1/assessment-journeys")
@Tag(name = "Assessment Journeys", description = "Autoria e publicação de Jornadas de Avaliação.")
public class AssessmentJourneyController {

    private final AssessmentJourneyService journeyService;
    private final AssessmentJourneyAttemptService journeyAttemptService;
    private final AuditEventService auditEventService;

    public AssessmentJourneyController(
            AssessmentJourneyService journeyService,
            AssessmentJourneyAttemptService journeyAttemptService,
            AuditEventService auditEventService
    ) {
        this.journeyService = journeyService;
        this.journeyAttemptService = journeyAttemptService;
        this.auditEventService = auditEventService;
    }

    /**
     * Cria uma jornada em rascunho.
     *
     * @param request nome e descrição da jornada
     * @return o detalhe da jornada criada
     */
    @PostMapping
    @Operation(summary = "Cria jornada em rascunho")
    public ResponseEntity<AssessmentJourneyDetailResponse> create(
            @Valid @RequestBody CreateAssessmentJourneyRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(journeyService.createJourney(request));
    }

    /**
     * Lista as jornadas do tenant.
     *
     * @return o resumo de cada jornada
     */
    @GetMapping
    @Operation(summary = "Lista jornadas do tenant")
    public ResponseEntity<List<AssessmentJourneySummaryResponse>> list() {
        return ResponseEntity.ok(journeyService.listJourneys());
    }

    /**
     * Detalha uma jornada com suas sequências e testes.
     *
     * @param journeyId identificador da jornada
     * @return o detalhe completo
     */
    @GetMapping("/{journeyId}")
    @Operation(summary = "Detalha jornada")
    public ResponseEntity<AssessmentJourneyDetailResponse> get(@PathVariable String journeyId) {
        return ResponseEntity.ok(journeyService.getJourney(journeyId));
    }

    /**
     * Atualiza os dados básicos de uma jornada em rascunho.
     *
     * @param journeyId identificador da jornada
     * @param request novos dados básicos
     * @return o detalhe atualizado
     */
    @PatchMapping("/{journeyId}")
    @Operation(summary = "Atualiza dados básicos da jornada")
    public ResponseEntity<AssessmentJourneyDetailResponse> update(
            @PathVariable String journeyId,
            @Valid @RequestBody UpdateAssessmentJourneyRequest request
    ) {
        return ResponseEntity.ok(journeyService.updateJourney(journeyId, request));
    }

    /**
     * Adiciona um teste publicado à jornada.
     *
     * @param journeyId identificador da jornada
     * @param request teste a adicionar
     * @return o detalhe atualizado
     */
    @PostMapping("/{journeyId}/steps")
    @Operation(summary = "Adiciona teste à jornada")
    public ResponseEntity<AssessmentJourneyDetailResponse> addStep(
            @PathVariable String journeyId,
            @Valid @RequestBody AddJourneyStepRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(journeyService.addStep(journeyId, request));
    }

    /**
     * Atualiza ordem, sequência ou obrigatoriedade de uma etapa.
     *
     * @param journeyId identificador da jornada
     * @param stepId identificador da etapa
     * @param request campos a atualizar
     * @return o detalhe atualizado
     */
    @PatchMapping("/{journeyId}/steps/{stepId}")
    @Operation(summary = "Atualiza etapa da jornada")
    public ResponseEntity<AssessmentJourneyDetailResponse> updateStep(
            @PathVariable String journeyId,
            @PathVariable Long stepId,
            @Valid @RequestBody UpdateJourneyStepRequest request
    ) {
        return ResponseEntity.ok(journeyService.updateStep(journeyId, stepId, request));
    }

    /**
     * Remove uma etapa enquanto a jornada estiver em rascunho.
     *
     * @param journeyId identificador da jornada
     * @param stepId identificador da etapa a remover
     * @return confirmação sem conteúdo
     */
    @DeleteMapping("/{journeyId}/steps/{stepId}")
    @Operation(summary = "Remove etapa da jornada")
    public ResponseEntity<Void> removeStep(
            @PathVariable String journeyId,
            @PathVariable Long stepId
    ) {
        journeyService.removeStep(journeyId, stepId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Publica a jornada após validar sua composição.
     *
     * @param journeyId identificador da jornada
     * @return o detalhe da jornada publicada
     */
    @PostMapping("/{journeyId}/publish")
    @Operation(summary = "Publica jornada")
    public ResponseEntity<AssessmentJourneyDetailResponse> publish(@PathVariable String journeyId) {
        return ResponseEntity.ok(journeyService.publishJourney(journeyId));
    }

    /**
     * Arquiva a jornada (mantém histórico, não gera novos links).
     *
     * @param journeyId identificador da jornada
     * @return o detalhe da jornada arquivada
     */
    @PostMapping("/{journeyId}/archive")
    @Operation(summary = "Arquiva jornada")
    public ResponseEntity<AssessmentJourneyDetailResponse> archive(@PathVariable String journeyId) {
        return ResponseEntity.ok(journeyService.archiveJourney(journeyId));
    }

    /**
     * Lista as tentativas de candidatos de uma jornada.
     *
     * @param journeyId identificador da jornada
     * @return o progresso de cada tentativa
     */
    @GetMapping("/{journeyId}/attempts")
    @Operation(summary = "Lista tentativas de candidatos da jornada")
    public ResponseEntity<List<AssessmentJourneyAttemptResponse>> listAttempts(@PathVariable String journeyId) {
        return ResponseEntity.ok(journeyAttemptService.listAttemptsByJourney(journeyId));
    }

    /**
     * Recupera a trilha de auditoria da jornada.
     *
     * @param journeyId identificador da jornada
     * @return os eventos de auditoria da jornada
     */
    @GetMapping("/{journeyId}/audit-events")
    @Operation(summary = "Trilha de auditoria da jornada")
    public ResponseEntity<List<AuditEventResponse>> auditEvents(@PathVariable String journeyId) {
        // Garante que a jornada pertence ao tenant antes de expor sua trilha.
        journeyService.getJourney(journeyId);
        return ResponseEntity.ok(auditEventService.listAssessmentJourneyEvents(journeyId));
    }
}
