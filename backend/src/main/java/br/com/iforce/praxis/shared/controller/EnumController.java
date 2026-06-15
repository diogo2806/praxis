package br.com.iforce.praxis.shared.controller;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.model.ResultDecision;
import br.com.iforce.praxis.gupy.model.ResultTier;
import br.com.iforce.praxis.shared.dto.EnumOptionResponse;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.model.ValidationIssueSeverity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/enums")
@Tag(name = "Enums", description = "Opções de enums expostas para selects e comboboxes do front.")
public class EnumController {

    @GetMapping("/attempt-status")
    @Operation(summary = "Lista status de tentativa", description = "Retorna value/label para AttemptStatus.")
    public ResponseEntity<List<EnumOptionResponse>> listAttemptStatus() {
        List<EnumOptionResponse> options = Arrays.stream(AttemptStatus.values())
                .map(status -> new EnumOptionResponse(status.name(), status.getDescricao()))
                .toList();

        return ResponseEntity.ok(options);
    }

    @GetMapping("/result-tier")
    @Operation(summary = "Lista tiers de resultado", description = "Retorna value/label para ResultTier.")
    public ResponseEntity<List<EnumOptionResponse>> listResultTier() {
        List<EnumOptionResponse> options = Arrays.stream(ResultTier.values())
                .map(tier -> new EnumOptionResponse(tier.name(), tier.getDescricao()))
                .toList();

        return ResponseEntity.ok(options);
    }

    @GetMapping("/result-decision")
    @Operation(summary = "Lista decisões de resultado", description = "Retorna value/label para ResultDecision.")
    public ResponseEntity<List<EnumOptionResponse>> listResultDecision() {
        List<EnumOptionResponse> options = Arrays.stream(ResultDecision.values())
                .map(decision -> new EnumOptionResponse(decision.name(), decision.getDescricao()))
                .toList();

        return ResponseEntity.ok(options);
    }

    @GetMapping("/audit-event-type")
    @Operation(summary = "Lista tipos de evento de auditoria", description = "Retorna value/label para AuditEventType.")
    public ResponseEntity<List<EnumOptionResponse>> listAuditEventType() {
        List<EnumOptionResponse> options = Arrays.stream(AuditEventType.values())
                .map(type -> new EnumOptionResponse(type.name(), type.getDescricao()))
                .toList();

        return ResponseEntity.ok(options);
    }

    @GetMapping("/simulation-version-status")
    @Operation(summary = "Lista status de versão", description = "Retorna value/label para SimulationVersionStatus.")
    public ResponseEntity<List<EnumOptionResponse>> listSimulationVersionStatus() {
        List<EnumOptionResponse> options = Arrays.stream(SimulationVersionStatus.values())
                .map(status -> new EnumOptionResponse(status.name(), status.getDescricao()))
                .toList();

        return ResponseEntity.ok(options);
    }

    @GetMapping("/validation-issue-severity")
    @Operation(summary = "Lista severidades de validação", description = "Retorna value/label para ValidationIssueSeverity.")
    public ResponseEntity<List<EnumOptionResponse>> listValidationIssueSeverity() {
        List<EnumOptionResponse> options = Arrays.stream(ValidationIssueSeverity.values())
                .map(severity -> new EnumOptionResponse(severity.name(), severity.getDescricao()))
                .toList();

        return ResponseEntity.ok(options);
    }
}
