package br.com.iforce.praxis.simulation.controller;

import br.com.iforce.praxis.simulation.dto.ArchiveSimulationResponse;
import br.com.iforce.praxis.simulation.dto.CloneSimulationVersionResponse;
import br.com.iforce.praxis.simulation.dto.GupyPreflightResponse;
import br.com.iforce.praxis.simulation.dto.PublishSimulationResponse;
import br.com.iforce.praxis.simulation.dto.RejectSimulationVersionRequest;
import br.com.iforce.praxis.simulation.dto.SimulationMonitoringResponse;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.dto.SimulationVersionStatusResponse;
import br.com.iforce.praxis.simulation.service.GupyPreflightService;
import br.com.iforce.praxis.simulation.service.SimulationAdminService;
import br.com.iforce.praxis.simulation.service.SimulationMonitoringService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "Simulations Admin", description = "Validação e publicação de versões de simulações.")
public class SimulationAdminController {

    private static final String ERROR_EXAMPLE = """
            {
              "timestamp": "2026-06-16T13:20:00Z",
              "status": 409,
              "error": "Conflict",
              "message": "Versao precisa estar aprovada antes da publicacao.",
              "path": "/api/v1/simulations/sim-atendimento-caos/versions/1/publish",
              "fields": {}
            }
            """;

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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Validacao executada."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationValidationResponse> validateVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationAdminService.validateVersion(simulationId, versionNumber));
    }

    @PostMapping("/{simulationId}/versions/{versionNumber}/clone-draft")
    @Operation(
            summary = "Clona versao publicada para edicao",
            description = "Cria a proxima versao em rascunho a partir de uma versao publicada, preservando a publicada imutavel."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rascunho clonado."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Versao nao pode ser clonada neste estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<CloneSimulationVersionResponse> clonePublishedVersionToDraft(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationAdminService.clonePublishedVersionToDraft(simulationId, versionNumber));
    }

    @PostMapping("/{simulationId}/versions/{versionNumber}/submit-review")
    @Operation(
            summary = "Envia versao para revisao",
            description = "Move uma versao em rascunho ou reprovada para revisao quando o validador nao possui blockers."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versao enviada para revisao."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Revisao bloqueada.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationVersionStatusResponse> submitVersionForReview(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationAdminService.submitVersionForReview(simulationId, versionNumber));
    }

    @PostMapping("/{simulationId}/versions/{versionNumber}/approve")
    @Operation(
            summary = "Aprova versao revisada",
            description = "Move uma versao em revisao para aprovada, liberando a publicacao."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versao aprovada."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Versao nao pode ser aprovada neste estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationVersionStatusResponse> approveVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationAdminService.approveVersion(simulationId, versionNumber));
    }

    @PostMapping("/{simulationId}/versions/{versionNumber}/reject")
    @Operation(
            summary = "Reprova versao revisada",
            description = "Move uma versao em revisao para reprovada com justificativa obrigatoria."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versao reprovada."),
            @ApiResponse(responseCode = "400", description = "Payload invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Versao nao pode ser reprovada neste estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationVersionStatusResponse> rejectVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @Valid @RequestBody RejectSimulationVersionRequest request
    ) {
        return ResponseEntity.ok(simulationAdminService.rejectVersion(simulationId, versionNumber, request.reason()));
    }

    @PostMapping("/{simulationId}/versions/{versionNumber}/publish")
    @Operation(
            summary = "Publica versão de simulação",
            description = "Publica somente versões sem blockers. Não existe override para blocker."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versao publicada."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Publicacao bloqueada.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preflight executado."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
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
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Indicadores retornados."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<SimulationMonitoringResponse> monitorVersion(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(simulationMonitoringService.getMonitoring(simulationId, versionNumber));
    }

    @DeleteMapping("/{simulationId}")
    @Operation(
            summary = "Arquiva simulacao",
            description = "Executa soft delete da simulacao, preenchendo deletedAt, deletedBy e archived sem apagar dados fisicos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulacao arquivada."),
            @ApiResponse(responseCode = "400", description = "Header ou parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Simulacao ja arquivada.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<ArchiveSimulationResponse> archiveSimulation(
            @PathVariable String simulationId,
            @RequestHeader(name = "X-User-Id") String deletedBy
    ) {
        return ResponseEntity.ok(simulationAdminService.archiveSimulation(simulationId, deletedBy));
    }
}
