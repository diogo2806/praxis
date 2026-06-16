package br.com.iforce.praxis.gupy.controller;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.dto.GupyTestResponse;
import br.com.iforce.praxis.gupy.dto.TestItemsResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.GupyAuthService;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Gupy Integration", description = "Endpoints REST consumidos pela Gupy para testes externos.")
public class GupyIntegrationController {

    private static final String ERROR_EXAMPLE = """
            {
              "timestamp": "2026-06-16T13:20:00Z",
              "status": 400,
              "error": "Bad Request",
              "message": "Dados invalidos.",
              "path": "/test/candidate",
              "fields": {
                "companyId": "nao deve estar em branco"
              }
            }
            """;

    private final GupyAuthService gupyAuthService;
    private final SimulationCatalogService simulationCatalogService;
    private final CandidateAttemptService candidateAttemptService;

    public GupyIntegrationController(
            GupyAuthService gupyAuthService,
            SimulationCatalogService simulationCatalogService,
            CandidateAttemptService candidateAttemptService
    ) {
        this.gupyAuthService = gupyAuthService;
        this.simulationCatalogService = simulationCatalogService;
        this.candidateAttemptService = candidateAttemptService;
    }

    @GetMapping("/test")
    @Operation(summary = "Lista simulações publicadas", description = "Retorna as simulações Práxis publicadas como Test[].")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulacoes retornadas."),
            @ApiResponse(responseCode = "400", description = "Requisicao invalida.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<TestItemsResponse> listPublishedTests(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(name = "searchString", required = false) String searchString,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        gupyAuthService.validateBearerToken(authorization);

        List<GupyTestResponse> tests = simulationCatalogService.findPublished(searchString, offset, limit).stream()
                .map(simulation -> new GupyTestResponse(
                        simulation.id(),
                        simulation.name(),
                        "Situational Judgment",
                        simulation.description(),
                        "professional"
                ))
                .toList();

        int totalTests = simulationCatalogService.countPublished(searchString);
        return ResponseEntity.ok(new TestItemsResponse(limit, offset, totalTests, tests));
    }

    @PostMapping("/test/candidate")
    @Operation(summary = "Registra candidato", description = "Cria ou reutiliza tentativa por company_id, document_id e test_id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tentativa criada ou reutilizada."),
            @ApiResponse(responseCode = "400", description = "Payload invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotencia ou estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<CreateCandidateResponse> createCandidateAttempt(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateCandidateRequest request
    ) {
        gupyAuthService.validateBearerToken(authorization);
        return ResponseEntity.ok(candidateAttemptService.createOrReuse(request));
    }

    @GetMapping("/test/result/{resultId}")
    @Operation(summary = "Consulta resultado", description = "Retorna o TestResult para a Gupy, incluindo score e competências.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado retornado."),
            @ApiResponse(responseCode = "400", description = "Parametro invalido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<TestResultResponse> getTestResult(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String resultId
    ) {
        gupyAuthService.validateBearerToken(authorization);
        return ResponseEntity.ok(candidateAttemptService.findResult(resultId));
    }
}
