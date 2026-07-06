package br.com.iforce.praxis.simulation.controller;

import br.com.iforce.praxis.simulation.dto.SimulationSummaryResponse;
import br.com.iforce.praxis.simulation.dto.UpdateAssessmentRequest;
import br.com.iforce.praxis.simulation.service.SimulationAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrato novo da API para atualizar a estrutura de uma avaliação.
 * A rota antiga com "blueprint" permanece temporariamente para clientes legados.
 */
@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Avaliações", description = "Estrutura e critérios das avaliações.")
public class AssessmentStructureController {

    private final SimulationAdminService simulationAdminService;

    public AssessmentStructureController(SimulationAdminService simulationAdminService) {
        this.simulationAdminService = simulationAdminService;
    }

    @PatchMapping("/{simulationId}/versions/{versionNumber}/assessment")
    @Operation(
            summary = "Atualiza a estrutura da avaliação",
            description = "Atualiza etapa inicial, competências e pesos de uma versão em rascunho."
    )
    public ResponseEntity<SimulationSummaryResponse> updateAssessment(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @Valid @RequestBody UpdateAssessmentRequest request
    ) {
        return ResponseEntity.ok(
                simulationAdminService.updateBlueprint(simulationId, versionNumber, request.toLegacyRequest())
        );
    }
}
