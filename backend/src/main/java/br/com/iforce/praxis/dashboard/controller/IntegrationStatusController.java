package br.com.iforce.praxis.dashboard.controller;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.IntegrationStatusItem;
import br.com.iforce.praxis.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/integrations/status")
public class IntegrationStatusController {

    private final DashboardService dashboardService;

    public IntegrationStatusController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<List<IntegrationStatusItem>> getStatus() {
        return ResponseEntity.ok(dashboardService.getIntegrationStatuses());
    }
}
