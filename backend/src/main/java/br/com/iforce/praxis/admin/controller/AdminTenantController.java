package br.com.iforce.praxis.admin.controller;

import br.com.iforce.praxis.admin.dto.AdminActionReasonRequest;
import br.com.iforce.praxis.admin.dto.CreateTenantAdminRequest;
import br.com.iforce.praxis.admin.dto.CreateTenantAdminResponse;
import br.com.iforce.praxis.admin.dto.ReactivateTenantAdminRequest;
import br.com.iforce.praxis.admin.dto.TenantAdminDetailResponse;
import br.com.iforce.praxis.admin.dto.TenantAdminSummaryResponse;
import br.com.iforce.praxis.admin.dto.TenantUsageResponse;
import br.com.iforce.praxis.admin.dto.UpdateTenantAdminRequest;
import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.admin.service.AdminAuditService;
import br.com.iforce.praxis.admin.service.AdminTenantService;
import br.com.iforce.praxis.admin.service.AdminUsageService;
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
 * API administrativa de clientes (tenants). Exige papel {@code ADMIN} e não depende do tenant
 * do usuário logado: o cliente alvo vem sempre explícito na rota.
 */
@RestController
@RequestMapping("/api/admin/tenants")
@Tag(name = "Admin · Clientes", description = "Cadastro e governança de clientes (tenants) pelo operador ADMIN.")
public class AdminTenantController {

    private final AdminTenantService adminTenantService;
    private final AdminUsageService adminUsageService;
    private final AdminAuditService adminAuditService;
    private final CurrentUserService currentUserService;

    public AdminTenantController(
            AdminTenantService adminTenantService,
            AdminUsageService adminUsageService,
            AdminAuditService adminAuditService,
            CurrentUserService currentUserService
    ) {
        this.adminTenantService = adminTenantService;
        this.adminUsageService = adminUsageService;
        this.adminAuditService = adminAuditService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    @Operation(summary = "Lista clientes", description = "Filtra por busca livre, status, plano comercial e período de uso.")
    public ResponseEntity<List<TenantAdminSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TenantStatus status,
            @RequestParam(required = false) CommercialPlanType plan,
            @RequestParam(required = false) String periodStart,
            @RequestParam(required = false) String periodEnd
    ) {
        return ResponseEntity.ok(adminTenantService.list(
                search, status, plan, parseInstant(periodStart), parseInstant(periodEnd)));
    }

    @PostMapping
    @Operation(summary = "Cadastra cliente", description = "Cria o tenant e o primeiro usuário responsável com papel EMPRESA.")
    public ResponseEntity<CreateTenantAdminResponse> create(@Valid @RequestBody CreateTenantAdminRequest request) {
        return ResponseEntity.ok(adminTenantService.create(currentUserService.requiredUserId(), request));
    }

    @GetMapping("/{tenantId}")
    @Operation(summary = "Detalha cliente")
    public ResponseEntity<TenantAdminDetailResponse> detail(@PathVariable String tenantId) {
        return ResponseEntity.ok(adminTenantService.detail(tenantId));
    }

    @PatchMapping("/{tenantId}")
    @Operation(summary = "Atualiza dados do cliente")
    public ResponseEntity<TenantAdminDetailResponse> update(
            @PathVariable String tenantId,
            @Valid @RequestBody UpdateTenantAdminRequest request
    ) {
        return ResponseEntity.ok(adminTenantService.update(
                currentUserService.requiredUserId(), tenantId, request));
    }

    @PostMapping("/{tenantId}/suspend")
    @Operation(summary = "Suspende cliente", description = "Exige motivo. Cliente suspenso não autentica nem consome APIs protegidas.")
    public ResponseEntity<TenantAdminDetailResponse> suspend(
            @PathVariable String tenantId,
            @Valid @RequestBody AdminActionReasonRequest request
    ) {
        return ResponseEntity.ok(adminTenantService.suspend(
                currentUserService.requiredUserId(), tenantId, request.reason()));
    }

    @PostMapping("/{tenantId}/reactivate")
    @Operation(summary = "Reativa cliente", description = "Exige motivo. Status alvo ATIVO ou EM_TESTE.")
    public ResponseEntity<TenantAdminDetailResponse> reactivate(
            @PathVariable String tenantId,
            @Valid @RequestBody ReactivateTenantAdminRequest request
    ) {
        return ResponseEntity.ok(adminTenantService.reactivate(
                currentUserService.requiredUserId(), tenantId, request));
    }

    @PostMapping("/{tenantId}/cancel")
    @Operation(summary = "Cancela cliente", description = "Exige motivo. Preserva todo o histórico.")
    public ResponseEntity<TenantAdminDetailResponse> cancel(
            @PathVariable String tenantId,
            @Valid @RequestBody AdminActionReasonRequest request
    ) {
        return ResponseEntity.ok(adminTenantService.cancel(
                currentUserService.requiredUserId(), tenantId, request.reason()));
    }

    @GetMapping("/{tenantId}/usage")
    @Operation(summary = "Uso do cliente", description = "Avaliações concluídas no período. Registra evento de auditoria.")
    public ResponseEntity<TenantUsageResponse> usage(
            @PathVariable String tenantId,
            @RequestParam(required = false) String periodStart,
            @RequestParam(required = false) String periodEnd
    ) {
        Instant end = periodEnd == null ? Instant.now() : parseInstant(periodEnd);
        Instant start = periodStart == null ? end.minusSeconds(30L * 24 * 60 * 60) : parseInstant(periodStart);
        adminAuditService.recordUsageViewed(currentUserService.requiredUserId(), tenantId);
        return ResponseEntity.ok(adminUsageService.usage(tenantId, start, end));
    }

    @GetMapping("/{tenantId}/audit")
    @Operation(summary = "Auditoria do cliente", description = "Eventos append-only ligados ao tenant (somente leitura).")
    public ResponseEntity<?> audit(
            @PathVariable String tenantId,
            @RequestParam(required = false, defaultValue = "200") int limit
    ) {
        return ResponseEntity.ok(adminAuditService.listForTenant(tenantId, limit));
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }
}
