package br.com.iforce.praxis.admin.controller;

import br.com.iforce.praxis.admin.dto.AdminActionReasonRequest;
import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminRequest;
import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminResponse;
import br.com.iforce.praxis.admin.dto.EmpresaAdminDetailResponse;
import br.com.iforce.praxis.admin.dto.EmpresaHealthResponse;
import br.com.iforce.praxis.admin.dto.EmpresaUsageResponse;
import br.com.iforce.praxis.admin.dto.GrantCreditsAdminRequest;
import br.com.iforce.praxis.admin.dto.ReactivateEmpresaAdminRequest;
import br.com.iforce.praxis.admin.dto.UpdateEmpresaAdminRequest;
import br.com.iforce.praxis.admin.service.AdminAuditService;
import br.com.iforce.praxis.admin.service.AdminEmpresaService;
import br.com.iforce.praxis.admin.service.AdminUsageService;
import br.com.iforce.praxis.admin.service.CustomerHealthService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/**
 * API administrativa de cadastro e governança dos clientes da plataforma.
 * A listagem é fornecida exclusivamente pelo endpoint paginado de
 * {@link AdminEmpresaQueryController}.
 */
@RestController
@RequestMapping("/api/admin/empresas")
@Tag(name = "Admin · Clientes", description = "Cadastro e governança de clientes pelo operador ADMIN.")
public class AdminEmpresaController {

    private final AdminEmpresaService adminEmpresaService;
    private final AdminUsageService adminUsageService;
    private final AdminAuditService adminAuditService;
    private final CustomerHealthService customerHealthService;
    private final CurrentUserService currentUserService;

    public AdminEmpresaController(
            AdminEmpresaService adminEmpresaService,
            AdminUsageService adminUsageService,
            AdminAuditService adminAuditService,
            CustomerHealthService customerHealthService,
            CurrentUserService currentUserService
    ) {
        this.adminEmpresaService = adminEmpresaService;
        this.adminUsageService = adminUsageService;
        this.adminAuditService = adminAuditService;
        this.customerHealthService = customerHealthService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/at-risk")
    @Operation(
            summary = "Fila de atuação de Customer Success",
            description = "Clientes ativos cuja utilização caiu além do limite configurado."
    )
    public ResponseEntity<List<EmpresaHealthResponse>> atRisk() {
        return ResponseEntity.ok(customerHealthService.atRiskEmpresas(Instant.now()));
    }

    @PostMapping
    @Operation(summary = "Cadastra cliente")
    public ResponseEntity<CreateEmpresaAdminResponse> create(
            @Valid @RequestBody CreateEmpresaAdminRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.create(
                currentUserService.requiredUserId(),
                request
        ));
    }

    @GetMapping("/{empresaId}")
    @Operation(summary = "Detalha cliente")
    public ResponseEntity<EmpresaAdminDetailResponse> detail(@PathVariable String empresaId) {
        return ResponseEntity.ok(adminEmpresaService.detail(empresaId));
    }

    @PatchMapping("/{empresaId}")
    @Operation(summary = "Atualiza dados do cliente")
    public ResponseEntity<EmpresaAdminDetailResponse> update(
            @PathVariable String empresaId,
            @Valid @RequestBody UpdateEmpresaAdminRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.update(
                currentUserService.requiredUserId(),
                empresaId,
                request
        ));
    }

    @PostMapping("/{empresaId}/suspend")
    @Operation(summary = "Suspende cliente")
    public ResponseEntity<EmpresaAdminDetailResponse> suspend(
            @PathVariable String empresaId,
            @Valid @RequestBody AdminActionReasonRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.suspend(
                currentUserService.requiredUserId(),
                empresaId,
                request.reason()
        ));
    }

    @PostMapping("/{empresaId}/reactivate")
    @Operation(summary = "Reativa cliente")
    public ResponseEntity<EmpresaAdminDetailResponse> reactivate(
            @PathVariable String empresaId,
            @Valid @RequestBody ReactivateEmpresaAdminRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.reactivate(
                currentUserService.requiredUserId(),
                empresaId,
                request
        ));
    }

    @PostMapping("/{empresaId}/cancel")
    @Operation(summary = "Cancela cliente")
    public ResponseEntity<EmpresaAdminDetailResponse> cancel(
            @PathVariable String empresaId,
            @Valid @RequestBody AdminActionReasonRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.cancel(
                currentUserService.requiredUserId(),
                empresaId,
                request.reason()
        ));
    }

    @PostMapping("/{empresaId}/credits")
    @Operation(summary = "Concede créditos de cortesia")
    public ResponseEntity<EmpresaAdminDetailResponse> grantCredits(
            @PathVariable String empresaId,
            @Valid @RequestBody GrantCreditsAdminRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.grantCredits(
                currentUserService.requiredUserId(),
                empresaId,
                request.amount(),
                request.note()
        ));
    }

    @GetMapping("/{empresaId}/usage")
    @Operation(summary = "Uso do cliente")
    public ResponseEntity<EmpresaUsageResponse> usage(
            @PathVariable String empresaId,
            @RequestParam(required = false) String periodStart,
            @RequestParam(required = false) String periodEnd
    ) {
        Instant end = periodEnd == null ? Instant.now() : parseInstant(periodEnd);
        Instant start = periodStart == null
                ? end.minusSeconds(30L * 24 * 60 * 60)
                : parseInstant(periodStart);
        adminAuditService.recordUsageViewed(
                currentUserService.requiredUserId(),
                empresaId
        );
        return ResponseEntity.ok(adminUsageService.usage(empresaId, start, end));
    }

    @GetMapping("/{empresaId}/audit")
    @Operation(summary = "Auditoria do cliente")
    public ResponseEntity<?> audit(
            @PathVariable String empresaId,
            @RequestParam(required = false, defaultValue = "200") int limit
    ) {
        return ResponseEntity.ok(adminAuditService.listForEmpresa(empresaId, limit));
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }
}
