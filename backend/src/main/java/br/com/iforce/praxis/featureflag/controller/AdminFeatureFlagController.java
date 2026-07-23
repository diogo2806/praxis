package br.com.iforce.praxis.featureflag.controller;

import br.com.iforce.praxis.admin.dto.AdminAuditEventResponse;
import br.com.iforce.praxis.admin.service.AdminAuditService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.EvaluationRequest;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.EvaluationResponse;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.GovernanceSummary;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.MetricRequest;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.MetricResponse;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.Response;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.ToggleRequest;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.UpsertRequest;
import br.com.iforce.praxis.featureflag.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/admin/feature-flags")
@Tag(name = "Admin · Feature flags", description = "Rollout progressivo, kill switch e governança por escopo.")
public class AdminFeatureFlagController {

    private final FeatureFlagService featureFlagService;
    private final CurrentUserService currentUserService;
    private final AdminAuditService adminAuditService;

    public AdminFeatureFlagController(
            FeatureFlagService featureFlagService,
            CurrentUserService currentUserService,
            AdminAuditService adminAuditService
    ) {
        this.featureFlagService = featureFlagService;
        this.currentUserService = currentUserService;
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    @Operation(summary = "Lista e resume a governança das flags")
    public ResponseEntity<GovernanceSummary> governance(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active
    ) {
        return ResponseEntity.ok(featureFlagService.governance(search, active));
    }

    @PostMapping
    @Operation(summary = "Cria uma feature flag")
    public ResponseEntity<Response> create(@Valid @RequestBody UpsertRequest request) {
        return ResponseEntity.ok(featureFlagService.create(currentUserService.requiredUserId(), request));
    }

    @PutMapping("/{flagId}")
    @Operation(summary = "Atualiza uma feature flag")
    public ResponseEntity<Response> update(
            @PathVariable String flagId,
            @Valid @RequestBody UpsertRequest request
    ) {
        return ResponseEntity.ok(featureFlagService.update(currentUserService.requiredUserId(), flagId, request));
    }

    @PostMapping("/{flagId}/active")
    @Operation(summary = "Ativa ou desativa sem novo deploy")
    public ResponseEntity<Response> active(
            @PathVariable String flagId,
            @Valid @RequestBody ToggleRequest request
    ) {
        return ResponseEntity.ok(featureFlagService.setActive(
                currentUserService.requiredUserId(),
                flagId,
                request.enabled()
        ));
    }

    @PostMapping("/{flagId}/kill-switch")
    @Operation(summary = "Aciona ou libera o kill switch")
    public ResponseEntity<Response> killSwitch(
            @PathVariable String flagId,
            @Valid @RequestBody ToggleRequest request
    ) {
        return ResponseEntity.ok(featureFlagService.setKillSwitch(
                currentUserService.requiredUserId(),
                flagId,
                request.enabled()
        ));
    }

    @PostMapping("/{flagId}/evaluate")
    @Operation(summary = "Simula a decisão para um contexto")
    public ResponseEntity<EvaluationResponse> evaluate(
            @PathVariable String flagId,
            @RequestBody(required = false) EvaluationRequest request
    ) {
        return ResponseEntity.ok(featureFlagService.evaluate(flagId, request));
    }

    @PostMapping("/{flagId}/metrics")
    @Operation(summary = "Registra amostra de erro, latência, abandono ou integração por variante")
    public ResponseEntity<MetricResponse> recordMetric(
            @PathVariable String flagId,
            @Valid @RequestBody MetricRequest request
    ) {
        return ResponseEntity.ok(featureFlagService.recordMetric(flagId, request));
    }

    @GetMapping("/{flagId}/metrics")
    @Operation(summary = "Lista métricas agregadas por variante")
    public ResponseEntity<List<MetricResponse>> metrics(@PathVariable String flagId) {
        return ResponseEntity.ok(featureFlagService.metrics(flagId));
    }

    @GetMapping("/{flagId}/history")
    @Operation(summary = "Lista o histórico append-only da flag")
    public ResponseEntity<List<AdminAuditEventResponse>> history(@PathVariable String flagId) {
        List<AdminAuditEventResponse> history = adminAuditService.listAll(1000).stream()
                .filter(event -> event.metadata() != null && event.metadata().contains(flagId))
                .toList();
        return ResponseEntity.ok(history);
    }
}
