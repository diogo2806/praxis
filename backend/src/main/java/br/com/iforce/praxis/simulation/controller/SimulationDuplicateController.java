package br.com.iforce.praxis.simulation.controller;

import br.com.iforce.praxis.simulation.dto.DuplicateSimulationRequest;
import br.com.iforce.praxis.simulation.dto.SimulationVersionDetailResponse;
import br.com.iforce.praxis.simulation.service.SimulationDuplicateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Operações de reutilização de avaliações criadas pela própria empresa. */
@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Simulation Templates", description = "Reutilização segura de avaliações da empresa.")
public class SimulationDuplicateController {

    private final SimulationDuplicateService simulationDuplicateService;

    public SimulationDuplicateController(SimulationDuplicateService simulationDuplicateService) {
        this.simulationDuplicateService = simulationDuplicateService;
    }

    @PostMapping("/{simulationId}/versions/{versionNumber}/duplicate")
    @Operation(
            summary = "Duplica uma avaliação como novo rascunho",
            description = "Copia competências, etapas, alternativas e pontuações para uma nova avaliação independente da mesma empresa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Avaliação duplicada."),
            @ApiResponse(responseCode = "400", description = "Nome inválido."),
            @ApiResponse(responseCode = "403", description = "Acesso negado."),
            @ApiResponse(responseCode = "404", description = "Avaliação de origem não encontrada.")
    })
    public ResponseEntity<SimulationVersionDetailResponse> duplicate(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @Valid @RequestBody DuplicateSimulationRequest request
    ) {
        return ResponseEntity.status(201).body(
                simulationDuplicateService.duplicate(simulationId, versionNumber, request)
        );
    }
}
