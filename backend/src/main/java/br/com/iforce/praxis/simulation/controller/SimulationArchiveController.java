package br.com.iforce.praxis.simulation.controller;

import br.com.iforce.praxis.simulation.service.SimulationArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Porta de entrada para retirar avaliações de uso sem apagar histórico.
 *
 * <p>Na visão do processo, arquivar é a ação segura para o RH quando uma
 * avaliação não deve mais ser aplicada. Diferente da exclusão física, preserva
 * evidências, resultados, versões e trilha de auditoria.</p>
 */
@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Simulations Archive", description = "Arquivamento seguro de avaliações sem exclusão definitiva.")
public class SimulationArchiveController {

    private final SimulationArchiveService simulationArchiveService;

    public SimulationArchiveController(SimulationArchiveService simulationArchiveService) {
        this.simulationArchiveService = simulationArchiveService;
    }

    /**
     * Arquiva uma avaliação e todas as suas versões.
     *
     * @param simulationId identificador da avaliação
     * @return confirmação sem conteúdo
     */
    @PostMapping("/{simulationId}/archive")
    @Operation(
            summary = "Arquiva uma avaliação",
            description = "Retira a avaliação de uso sem apagar versões, tentativas, resultados ou auditoria."
    )
    public ResponseEntity<Void> archiveSimulation(@PathVariable String simulationId) {
        simulationArchiveService.archiveSimulation(simulationId);
        return ResponseEntity.noContent().build();
    }
}
