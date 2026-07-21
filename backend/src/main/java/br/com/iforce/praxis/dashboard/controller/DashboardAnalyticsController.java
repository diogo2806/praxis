package br.com.iforce.praxis.dashboard.controller;

import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse;
import br.com.iforce.praxis.dashboard.service.DashboardAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe os indicadores visuais usados pelo dashboard da empresa.
 */
@RestController
@RequestMapping("/api/v1/dashboard/analytics")
@Tag(name = "Dashboard", description = "Indicadores consolidados da operação da empresa")
public class DashboardAnalyticsController {

    private final DashboardAnalyticsService dashboardAnalyticsService;

    public DashboardAnalyticsController(DashboardAnalyticsService dashboardAnalyticsService) {
        this.dashboardAnalyticsService = dashboardAnalyticsService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar indicadores analíticos do dashboard",
            description = "Retorna funil, taxas e atividade diária da empresa autenticada nos últimos 30 dias."
    )
    public ResponseEntity<DashboardAnalyticsResponse> getAnalytics() {
        return ResponseEntity.ok(dashboardAnalyticsService.getAnalytics());
    }
}
