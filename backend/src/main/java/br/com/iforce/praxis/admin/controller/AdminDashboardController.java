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
