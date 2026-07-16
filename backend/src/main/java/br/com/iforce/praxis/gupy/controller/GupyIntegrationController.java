package br.com.iforce.praxis.gupy.controller;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.dto.GupyTestResponse;
import br.com.iforce.praxis.gupy.dto.TestItemsResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.GupyTestCatalogMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.integration.IntegrationAuthService;
import br.com.iforce.praxis.shared.integration.IntegrationEmpresaContext;
import br.com.iforce.praxis.shared.integration.IntegrationManagementService;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Porta de entrada (API) usada pela Gupy para integrar com a Práxis.
 *
 * <p>Na visão do processo, é por aqui que a plataforma de recrutamento Gupy
 * conversa com a Práxis: lista as provas (simulações) publicadas, cadastra um
 * candidato para fazer a prova e consulta o resultado depois de pronto. Cada
 * chamada precisa apresentar um token de acesso válido, que identifica de qual
 * empresa (empresa) é a requisição, garantindo o isolamento dos dados.</p>
 */
@RestController
@Transactional
@Tag(name = "Gupy Integration", description = "Endpoints REST consumidos pela Gupy para testes externos.")
public class GupyIntegrationController {

    private static final String PROVIDER = "gupy";
    private static final String LIST_TESTS_EVIDENCE = "GET /test";
    private static final String CREATE_CANDIDATE_EVIDENCE = "POST /test/candidate";
    private static final String GET_RESULT_EVIDENCE = "GET /test/result/{resultId}";
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

    private final IntegrationAuthService integrationAuthService;
    private final SimulationCatalogService simulationCatalogService;
    private final GupyTestCatalogMapper gupyTestCatalogMapper;
    private final CandidateAttemptService candidateAttemptService;
    private final IntegrationManagementService integrationManagementService;

    public GupyIntegrationController(
            IntegrationAuthService integrationAuthService,
            SimulationCatalogService simulationCatalogService,
            GupyTestCatalogMapper gupyTestCatalogMapper,
            CandidateAttemptService candidateAttemptService,
            IntegrationManagementService integrationManagementService
    ) {
        this.integrationAuthService = integrationAuthService;
        this.simulationCatalogService = simulationCatalogService;
        this.gupyTestCatalogMapper = gupyTestCatalogMapper;
        this.candidateAttemptService = candidateAttemptService;
        this.integrationManagementService = integrationManagementService;
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
        IntegrationEmpresaContext empresaContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);
        int normalizedOffset = Math.max(offset, 0);
        int normalizedLimit = Math.min(Math.max(limit, 0), 400);

        List<GupyTestResponse> tests = normalizedLimit == 0
                ? List.of()
                : simulationCatalogService
                        .findPublished(empresaContext.empresaId(), searchString, normalizedOffset, normalizedLimit).stream()
                        .map(gupyTestCatalogMapper::toResponse)
                        .toList();

        int totalTests = simulationCatalogService.countPublished(empresaContext.empresaId(), searchString);
        TestItemsResponse response = new TestItemsResponse(normalizedLimit, normalizedOffset, totalTests, tests);
        recordAuthenticatedActivity(empresaContext, LIST_TESTS_EVIDENCE);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test/candidate")
    @Operation(summary = "Registra candidato", description = "Cria ou reutiliza tentativa por company_id, document_id e test_id.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tentativa criada ou reutilizada."),
            @ApiResponse(responseCode = "400", description = "Dados invalidos.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de idempotencia ou estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<CreateCandidateResponse> createCandidateAttempt(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateCandidateRequest request
    ) {
        IntegrationEmpresaContext empresaContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);
        CreateCandidateResponse response = candidateAttemptService.createOrReuse(request, empresaContext);
        recordAuthenticatedActivity(empresaContext, CREATE_CANDIDATE_EVIDENCE);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/test/result/{resultId}")
    @Operation(summary = "Consulta resultado", description = "Retorna o resultado do teste para a Gupy, incluindo pontuação e competências.")
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
        IntegrationEmpresaContext empresaContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);
        TestResultResponse response = candidateAttemptService.findResult(
                resultId,
                empresaContext.companyId(),
                empresaContext
        );
        recordAuthenticatedActivity(empresaContext, GET_RESULT_EVIDENCE);
        return ResponseEntity.ok(response);
    }

    private void recordAuthenticatedActivity(
            IntegrationEmpresaContext empresaContext,
            String endpointEvidence
    ) {
        integrationManagementService.recordActivity(
                empresaContext.empresaId(),
                IntegrationProvider.GUPY,
                endpointEvidence
        );
    }
}
