package br.com.iforce.praxis.audit.controller;

import br.com.iforce.praxis.audit.dto.AuditEventResponse;

import br.com.iforce.praxis.audit.service.AuditEventService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


import java.util.List;


/**
 * Porta de entrada (API) para consultar a trilha de auditoria.
 *
 * <p>Na visão do processo, é por aqui que as telas administrativas pedem o
 * histórico de tudo o que aconteceu com uma prova de candidato ou com uma
 * versão de simulação. Serve para investigar problemas, comprovar
 * conformidade e dar transparência sobre cada passo do fluxo. Apenas
 * consulta — nunca altera o histórico.</p>
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Leitura da trilha cronol?gica de auditoria operacional.")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    /**
     * Lista, em ordem cronológica, tudo o que aconteceu com a prova de um
     * candidato (quando começou, respondeu, terminou, foi anonimizada, etc.).
     *
     * @param attemptId identificador da tentativa do candidato
     * @return o histórico de eventos daquela tentativa
     */
    @GetMapping("/candidate-attempts/{attemptId}")
    @Operation(
            summary = "Lista auditoria da tentativa",
            description = "Retorna eventos append-only registrados para criação, resposta e finalização da tentativa."
    )
    public ResponseEntity<List<AuditEventResponse>> listCandidateAttemptEvents(@PathVariable String attemptId) {
        return ResponseEntity.ok(auditEventService.listCandidateAttemptEvents(attemptId));
    }

    /**
     * Lista o histórico de mudanças de uma versão específica de uma prova
     * (criação, publicação e demais transições de estado daquela versão).
     *
     * @param simulationId identificador da simulação (prova)
     * @param versionNumber número da versão consultada
     * @return o histórico de eventos daquela versão
     */
    @GetMapping("/simulations/{simulationId}/versions/{versionNumber}")
    @Operation(
            summary = "Lista auditoria da vers?o",
            description = "Retorna eventos append-only registrados para transi??es de estado da vers?o do teste."
    )
    public ResponseEntity<List<AuditEventResponse>> listSimulationVersionEvents(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(auditEventService.listSimulationVersionEvents(simulationId, versionNumber));
    }
}
