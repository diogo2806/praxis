package br.com.iforce.praxis.simulation.controller;

import br.com.iforce.praxis.simulation.dto.PublishSimulationResponse;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.service.SimulationAdminService;
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

    public SimulationAdminController(SimulationAdminService simulationAdminService) {
        this.simulationAdminService = simulationAdminService;
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
}
