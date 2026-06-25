package br.com.iforce.praxis.gupy.controller;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.dto.GupyTestResponse;
import br.com.iforce.praxis.gupy.dto.TestItemsResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.integration.IntegrationAuthService;
import br.com.iforce.praxis.shared.integration.IntegrationTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
 * empresa (tenant) é a requisição, garantindo o isolamento dos dados.</p>
 */
@RestController
@Tag(name = "Gupy Integration", description = "Endpoints REST consumidos pela Gupy para testes externos.")
public class GupyIntegrationController {

    private static final String PROVIDER = "gupy";
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
    private final CandidateAttemptService candidateAttemptService;

    public GupyIntegrationController(
            IntegrationAuthService integrationAuthService,
            SimulationCatalogService simulationCatalogService,
            CandidateAttemptService candidateAttemptService
    ) {
        this.integrationAuthService = integrationAuthService;
        this.simulationCatalogService = simulationCatalogService;
        this.candidateAttemptService = candidateAttemptService;
    }

    /**
     * Lista, para a Gupy, as provas que estão publicadas e disponíveis.
     *
     * <p>Permite buscar por texto e paginar os resultados. Antes de tudo,
     * valida o token de acesso para descobrir a empresa solicitante.</p>
     *
     * @param authorization token de acesso da integração (cabeçalho HTTP)
     * @param searchString texto opcional para filtrar as provas pelo nome
     * @param offset a partir de qual posição começar a listagem (paginação)
     * @param limit quantas provas trazer por página
     * @return a lista de provas publicadas, no formato esperado pela Gupy
     */
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
        IntegrationTenantContext tenantContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);

        int normalizedOffset = Math.max(offset, 0);
        int normalizedLimit = Math.min(Math.max(limit, 1), 400);

        List<GupyTestResponse> tests = simulationCatalogService
                .findPublished(tenantContext.tenantId(), searchString, normalizedOffset, normalizedLimit).stream()
                .map(simulation -> new GupyTestResponse(
                        simulation.id(),
                        simulation.name(),
                        "Situational Judgment",
                        simulation.description(),
                        "advanced"
                ))
                .toList();

        int totalTests = simulationCatalogService.countPublished(tenantContext.tenantId(), searchString);
        return ResponseEntity.ok(new TestItemsResponse(normalizedLimit, normalizedOffset, totalTests, tests));
    }

    /**
     * Registra um candidato para fazer uma prova (vindo da Gupy).
     *
     * <p>Se o mesmo candidato (mesma empresa, documento e prova) já tiver uma
     * participação, ela é reaproveitada em vez de criar outra — evitando
     * duplicidade. Devolve os dados da participação criada ou reutilizada.</p>
     *
     * @param authorization token de acesso da integração (cabeçalho HTTP)
     * @param request dados do candidato e da prova a aplicar
     * @return a participação criada ou reaproveitada
     */
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
        IntegrationTenantContext tenantContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);
        return ResponseEntity.status(HttpStatus.CREATED).body(candidateAttemptService.createOrReuse(request, tenantContext));
    }

    /**
     * Consulta o resultado de uma prova já finalizada (para a Gupy).
     *
     * <p>Devolve a pontuação e o desempenho por competência do candidato.
     * Exige a empresa (company_id) para garantir que o resultado pertence a
     * quem está consultando.</p>
     *
     * @param authorization token de acesso da integração (cabeçalho HTTP)
     * @param resultId identificador do resultado consultado
     * @param companyId identificador da empresa dona do resultado
     * @return o resultado da prova no formato esperado pela Gupy
     */
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
            @PathVariable String resultId,
            @RequestParam(name = "company_id") String companyId
    ) {
        IntegrationTenantContext tenantContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);
        return ResponseEntity.ok(candidateAttemptService.findResult(resultId, companyId, tenantContext));
    }
}
