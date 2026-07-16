package br.com.iforce.praxis.admin.controller;

import br.com.iforce.praxis.admin.dto.EmpresaAdminPageResponse;
import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.EmpresaStatus;
import br.com.iforce.praxis.admin.service.AdminEmpresaQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/empresas")
@Tag(name = "Admin · Clientes", description = "Consulta paginada e eficiente de clientes da plataforma.")
public class AdminEmpresaQueryController {

    private final AdminEmpresaQueryService adminEmpresaQueryService;

    public AdminEmpresaQueryController(AdminEmpresaQueryService adminEmpresaQueryService) {
        this.adminEmpresaQueryService = adminEmpresaQueryService;
    }

    @GetMapping("/page")
    @Operation(
            summary = "Pesquisa clientes com paginação",
            description = "Retorna clientes com uso e saldo agregados em lote, sem consultas adicionais por linha."
    )
    public ResponseEntity<EmpresaAdminPageResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EmpresaStatus status,
            @RequestParam(required = false) CommercialPlanType plan,
            @RequestParam(required = false) String periodStart,
            @RequestParam(required = false) String periodEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ResponseEntity.ok(adminEmpresaQueryService.search(
                search,
                status,
                plan,
                parseInstant(periodStart),
                parseInstant(periodEnd),
                page,
                size
        ));
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }
}
