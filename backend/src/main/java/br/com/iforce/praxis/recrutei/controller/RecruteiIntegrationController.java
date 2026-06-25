package br.com.iforce.praxis.recrutei.controller;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.recrutei.dto.RecruteiCreateCandidateRequest;
import br.com.iforce.praxis.recrutei.dto.RecruteiCreateCandidateResponse;
import br.com.iforce.praxis.recrutei.dto.RecruteiTestListResponse;
import br.com.iforce.praxis.recrutei.dto.RecruteiTestResponse;
import br.com.iforce.praxis.recrutei.dto.RecruteiTestResultResponse;
import br.com.iforce.praxis.recrutei.service.RecruteiTestResultMapper;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Porta de entrada (API) usada pela Recrutei para integrar com a Práxis.
 *
 * <p>Na visão do processo, funciona como a integração com a Gupy, mas para a
 * plataforma Recrutei: lista as provas publicadas, cadastra um candidato para
 * fazer a prova e consulta o resultado depois de pronto — sempre com um token
 * de acesso que identifica a empresa solicitante. Internamente, reaproveita a
 * mesma lógica de participação usada pela Gupy, apenas traduzindo os dados
 * para o formato esperado pela Recrutei.</p>
 */
@RestController
@RequestMapping("/recrutei")
@Tag(name = "Recrutei Integration", description = "Endpoints REST consumidos pela Recrutei para testes externos.")
public class RecruteiIntegrationController {

    private static final String PROVIDER = "recrutei";
    private static final String ERROR_EXAMPLE = """
            {
              "timestamp": "2026-06-21T13:20:00Z",
              "status": 400,
              "error": "Bad Request",
              "message": "Dados invalidos.",
              "path": "/recrutei/test/candidate",
              "fields": {
                "companyId": "nao deve estar em branco"
              }
            }
            """;

    private final IntegrationAuthService integrationAuthService;
    private final SimulationCatalogService simulationCatalogService;
    private final CandidateAttemptService candidateAttemptService;
    private final RecruteiTestResultMapper recruteiTestResultMapper;

    public RecruteiIntegrationController(
            IntegrationAuthService integrationAuthService,
            SimulationCatalogService simulationCatalogService,
            CandidateAttemptService candidateAttemptService,
            RecruteiTestResultMapper recruteiTestResultMapper
    ) {
        this.integrationAuthService = integrationAuthService;
        this.simulationCatalogService = simulationCatalogService;
        this.candidateAttemptService = candidateAttemptService;
        this.recruteiTestResultMapper = recruteiTestResultMapper;
    }

    /**
     * Lista, para a Recrutei, as provas publicadas e disponíveis.
     *
     * <p>Permite buscar por texto e paginar. Valida antes o token para saber
     * de qual empresa é a requisição.</p>
     *
     * @param authorization token de acesso da integração (cabeçalho HTTP)
     * @param search texto opcional para filtrar as provas
     * @param offset a partir de qual posição começar (paginação)
     * @param limit quantas provas trazer por página
     * @return a lista de provas publicadas no formato da Recrutei
     */
    @GetMapping("/test")
    @Operation(summary = "Lista simulações publicadas", description = "Retorna as simulações Praxis publicadas para a Recrutei.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulações retornadas."),
            @ApiResponse(responseCode = "401", description = "Token inválido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "403", description = "Acesso negado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<RecruteiTestListResponse> listPublishedTests(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "offset", defaultValue = "0") int offset,
            @RequestParam(name = "limit", defaultValue = "50") int limit
    ) {
        IntegrationTenantContext tenantContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);

        int normalizedOffset = Math.max(offset, 0);
        int normalizedLimit = Math.min(Math.max(limit, 1), 400);

        List<RecruteiTestResponse> tests = simulationCatalogService
                .findPublished(tenantContext.tenantId(), search, normalizedOffset, normalizedLimit).stream()
                .map(simulation -> new RecruteiTestResponse(
                        simulation.id(),
                        simulation.name(),
                        "Situational Judgment",
                        simulation.description(),
                        "advanced"
                ))
                .toList();

        int total = simulationCatalogService.countPublished(tenantContext.tenantId(), search);
        return ResponseEntity.ok(new RecruteiTestListResponse(normalizedLimit, normalizedOffset, total, tests));
    }

    /**
     * Registra um candidato para fazer uma prova (vindo da Recrutei).
     *
     * <p>Cria a participação ou reaproveita uma existente para o mesmo
     * candidato e prova, evitando duplicidade. Devolve o link da prova e os
     * identificadores no formato da Recrutei.</p>
     *
     * @param authorization token de acesso da integração (cabeçalho HTTP)
     * @param request dados do candidato e da prova a aplicar
     * @return a participação criada ou reaproveitada, no formato da Recrutei
     */
    @PostMapping("/test/candidate")
    @Operation(summary = "Registra candidato", description = "Cria ou reutiliza tentativa por company_id, candidate_id e test_id.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tentativa criada ou reutilizada."),
            @ApiResponse(responseCode = "400", description = "Dados inválidos.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "Token inválido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "409", description = "Conflito de estado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<RecruteiCreateCandidateResponse> createCandidateAttempt(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @Valid @RequestBody RecruteiCreateCandidateRequest request
    ) {
        IntegrationTenantContext tenantContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);

        CreateCandidateRequest gupyRequest = new CreateCandidateRequest(
                request.companyId(),
                request.candidateId(),
                request.testId(),
                request.candidateName(),
                request.candidateEmail(),
                request.resultWebhookUrl(),
                request.accommodationTimeMultiplier(),
                null,
                null
        );

        CreateCandidateResponse response = candidateAttemptService.createOrReuse(gupyRequest, tenantContext);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new RecruteiCreateCandidateResponse(
                        response.testUrl(),
                        response.testResultId(),
                        request.vacancyId()
                )
        );
    }

    /**
     * Consulta o resultado de uma prova já finalizada (para a Recrutei).
     *
     * <p>Devolve a pontuação e o desempenho por competência. Exige a empresa
     * (company_id) para garantir que o resultado pertence a quem consulta.</p>
     *
     * @param authorization token de acesso da integração (cabeçalho HTTP)
     * @param resultId identificador do resultado consultado
     * @param companyId identificador da empresa dona do resultado
     * @return o resultado da prova no formato da Recrutei
     */
    @GetMapping("/test/result/{resultId}")
    @Operation(summary = "Consulta resultado", description = "Retorna o resultado do teste para a Recrutei, incluindo pontuação e competências.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resultado retornado."),
            @ApiResponse(responseCode = "400", description = "Parâmetro inválido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "401", description = "Token inválido.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE))),
            @ApiResponse(responseCode = "404", description = "Resultado não encontrado.", content = @Content(examples = @ExampleObject(value = ERROR_EXAMPLE)))
    })
    public ResponseEntity<RecruteiTestResultResponse> getTestResult(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @PathVariable String resultId,
            @RequestParam(name = "company_id") String companyId
    ) {
        IntegrationTenantContext tenantContext = integrationAuthService.validateBearerToken(authorization, PROVIDER);

        CandidateAttemptService.AttemptWithSimulation result =
                candidateAttemptService.findAttemptResult(resultId, companyId, tenantContext);

        return ResponseEntity.ok(
                recruteiTestResultMapper.toResponse(result.attempt(), result.simulation())
        );
    }
}
