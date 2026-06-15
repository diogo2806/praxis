package br.com.iforce.praxis.gupy.controller;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.dto.GupyTestResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.GupyAuthService;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Gupy Integration", description = "Endpoints REST consumidos pela Gupy para testes externos.")
public class GupyIntegrationController {

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
    public ResponseEntity<List<GupyTestResponse>> listPublishedTests(
            @RequestHeader(name = "Authorization", required = false) String authorization
    ) {
        gupyAuthService.validateBearerToken(authorization);

        List<GupyTestResponse> tests = simulationCatalogService.findPublished().stream()
                .map(simulation -> new GupyTestResponse(simulation.id(), simulation.name(), simulation.description()))
                .toList();

        return ResponseEntity.ok(tests);
    }

    @PostMapping("/test/candidate")
    @Operation(summary = "Registra candidato", description = "Cria ou reutiliza tentativa por company_id, document_id e test_id.")
    public ResponseEntity<CreateCandidateResponse> createCandidateAttempt(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateCandidateRequest request
    ) {
        gupyAuthService.validateBearerToken(authorization);
        return ResponseEntity.ok(candidateAttemptService.createOrReuse(request));
    }

    @GetMapping("/test/result/{resultId}")
    @Operation(summary = "Consulta resultado", description = "Retorna o TestResult para a Gupy, incluindo score e competências.")
    public ResponseEntity<TestResultResponse> getTestResult(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String resultId
    ) {
        gupyAuthService.validateBearerToken(authorization);
        return ResponseEntity.ok(candidateAttemptService.findResult(resultId));
    }
}
