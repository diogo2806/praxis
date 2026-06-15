package br.com.iforce.praxis.simulation.controller;

import br.com.iforce.praxis.simulation.dto.GupyPreflightResponse;
import br.com.iforce.praxis.simulation.dto.PublishSimulationResponse;
import br.com.iforce.praxis.simulation.dto.SimulationMonitoringResponse;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.service.GupyPreflightService;
import br.com.iforce.praxis.simulation.service.SimulationAdminService;
import br.com.iforce.praxis.simulation.service.SimulationMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Simulations Admin", description = "Validação e publicação de versões de simulações.")
public class SimulationAdminController {

    private final SimulationAdminService simulationAdminService;
    private final SimulationMonitoringService simulationMonitoringService;
    private final GupyPreflightService gupyPreflightService;

    public SimulationAdminController(
            SimulationAdminService simulationAdminService,
            SimulationMonitoringService simulationMonitoringService,
            GupyPreflightService gupyPreflightService
    ) {
        this.simulationAdminService = simulationAdminService;
        this.simulationMonitoringService = simulationMonitoringService;
        this.gupyPreflightService = gupyPreflightService;
    }

    @GetMapping("/{simulationId}/versions/{versionNumber}/validation")
    @Operation(
            summary = "Valida versão de simulação",
            description = "Executa validação determinística do grafo antes da publicação."
    )
    public ResponseEntity<SimulationValidationResponse> validateVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationAdminService.validateVersion(simulationId, versionNumber));
    }

    @PostMapping("/{simulationId}/versions/{versionNumber}/publish")
    @Operation(
            summary = "Publica versão de simulação",
            description = "Publica somente versões sem blockers. Não existe override para blocker."
    )
    public ResponseEntity<PublishSimulationResponse> publishVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationAdminService.publishVersion(simulationId, versionNumber));
    }

    @GetMapping("/{simulationId}/versions/{versionNumber}/gupy-preflight")
    @Operation(
            summary = "Executa preflight Gupy",
            description = "Valida configuracao publica, token de integracao e grafo antes da publicacao."
    )
    public ResponseEntity<GupyPreflightResponse> runGupyPreflight(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(gupyPreflightService.getPreflight(simulationId, versionNumber));
    }

    @GetMapping("/{simulationId}/versions/{versionNumber}/monitoring")
    @Operation(
            summary = "Monitora versao publicada",
            description = "Retorna indicadores de execucao, abandono e entrega de resultados para uma versao de simulacao."
    )
    public ResponseEntity<SimulationMonitoringResponse> monitorVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationMonitoringService.getMonitoring(simulationId, versionNumber));
    }
}
