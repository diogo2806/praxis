package br.com.iforce.praxis.admin.controller;

import br.com.iforce.praxis.admin.dto.AdminDashboardResponse;

import br.com.iforce.praxis.admin.service.AdminDashboardService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;


import java.time.Instant;


/** Dashboard administrativo (rota {@code /admin}). Exige papel {@code ADMIN}. */
@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "Admin · Dashboard", description = "Indicadores consolidados da plataforma para o ADMIN.")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    /**
     * Devolve os indicadores consolidados da tela inicial administrativa.
     *
     * <p>Na visão do processo: é a "tela de resumo" da operação — contagem de clientes por
     * situação, uso total no período, ranking de uso, clientes recentes e os que exigem
     * atenção. Sem período informado, usa o intervalo padrão.</p>
     *
     * @param periodStart início do período, em data/hora ISO (opcional)
     * @param periodEnd fim do período, em data/hora ISO (opcional)
     * @return os indicadores do painel administrativo
     */
    @GetMapping
    @Operation(summary = "Indicadores do dashboard")
    public ResponseEntity<AdminDashboardResponse> dashboard(
            @RequestParam(required = false) String periodStart,
            @RequestParam(required = false) String periodEnd
    ) {
        return ResponseEntity.ok(adminDashboardService.dashboard(
                parseInstant(periodStart), parseInstant(periodEnd)));
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }
}
