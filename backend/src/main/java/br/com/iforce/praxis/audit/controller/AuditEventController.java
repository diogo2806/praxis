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

@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Audit", description = "Leitura da trilha imutável de auditoria operacional.")
public class AuditEventController {

    private final AuditEventService auditEventService;

    public AuditEventController(AuditEventService auditEventService) {
        this.auditEventService = auditEventService;
    }

    @GetMapping("/candidate-attempts/{attemptId}")
    @Operation(
            summary = "Lista auditoria da tentativa",
            description = "Retorna eventos append-only registrados para criação, resposta e finalização da tentativa."
    )
    public ResponseEntity<List<AuditEventResponse>> listCandidateAttemptEvents(@PathVariable String attemptId) {
        return ResponseEntity.ok(auditEventService.listCandidateAttemptEvents(attemptId));
    }
}
