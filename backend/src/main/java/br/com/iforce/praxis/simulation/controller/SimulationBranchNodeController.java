package br.com.iforce.praxis.simulation.controller;

import br.com.iforce.praxis.simulation.service.SimulationBranchNodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Simulations Admin", description = "Validação e publicação de versões de simulações.")
public class SimulationBranchNodeController {

    private final SimulationBranchNodeService simulationBranchNodeService;

    public SimulationBranchNodeController(SimulationBranchNodeService simulationBranchNodeService) {
        this.simulationBranchNodeService = simulationBranchNodeService;
    }

    @PostMapping(
            "/{simulationId}/versions/{versionNumber}/nodes/{nodeId}/options/{optionId}/branch"
    )
    @Operation(
            summary = "Cria etapa ramificada",
            description = "Cria uma etapa vazia, vincula a resposta de origem e posiciona a nova ramificação no mapa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Etapa ramificada criada e vinculada."),
            @ApiResponse(responseCode = "404", description = "Versão, etapa ou resposta não encontrada."),
            @ApiResponse(responseCode = "409", description = "A versão não está em rascunho ou a etapa é final.")
    })
    public ResponseEntity<String> createBranchNode(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable String nodeId,
            @PathVariable String optionId
    ) {
        String createdNodeId = simulationBranchNodeService.createBranchNode(
                simulationId,
                versionNumber,
                nodeId,
                optionId
        );
        return ResponseEntity.status(201).body(createdNodeId);
    }
}
