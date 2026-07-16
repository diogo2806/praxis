package br.com.iforce.praxis.recrutei.controller;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.model.CandidateAttempt;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.recrutei.dto.RecruteiCreateCandidateRequest;
import br.com.iforce.praxis.recrutei.dto.RecruteiTestResultResponse;
import br.com.iforce.praxis.recrutei.service.RecruteiTestResultMapper;
import br.com.iforce.praxis.shared.integration.IntegrationAuthService;
import br.com.iforce.praxis.shared.integration.IntegrationEmpresaContext;
import br.com.iforce.praxis.shared.integration.IntegrationManagementService;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecruteiIntegrationActivityControllerTest {

    private static final String AUTHORIZATION = "Bearer token";
    private static final IntegrationEmpresaContext CONTEXT =
            new IntegrationEmpresaContext("empresa-1", "1", "recrutei");

    private final IntegrationAuthService integrationAuthService = mock(IntegrationAuthService.class);
    private final SimulationCatalogService simulationCatalogService = mock(SimulationCatalogService.class);
    private final CandidateAttemptService candidateAttemptService = mock(CandidateAttemptService.class);
    private final RecruteiTestResultMapper recruteiTestResultMapper = mock(RecruteiTestResultMapper.class);
    private final IntegrationManagementService integrationManagementService = mock(IntegrationManagementService.class);
    private final RecruteiIntegrationController controller = new RecruteiIntegrationController(
            integrationAuthService,
            simulationCatalogService,
            candidateAttemptService,
            recruteiTestResultMapper,
            integrationManagementService
    );

    @Test
    void listPublishedTestsRecordsAuthenticatedEndpointAfterSuccess() {
        when(integrationAuthService.validateBearerToken(AUTHORIZATION, "recrutei")).thenReturn(CONTEXT);
        when(simulationCatalogService.findPublished("empresa-1", null, 0, 50)).thenReturn(java.util.List.of());
        when(simulationCatalogService.countPublished("empresa-1", null)).thenReturn(0);

        var response = controller.listPublishedTests(AUTHORIZATION, null, 0, 50);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(integrationManagementService).recordActivity(
                "empresa-1",
                IntegrationProvider.RECRUTEI,
                "GET /recrutei/test"
        );
    }

    @Test
    void createCandidateRecordsAuthenticatedEndpointAndPreservesVacancyScope() {
        RecruteiCreateCandidateRequest request = new RecruteiCreateCandidateRequest(
                "1",
                "candidate-1",
                "test-1",
                "Candidato Teste",
                "candidato@example.com",
                "vacancy-1",
                URI.create("https://recrutei.example/result"),
                BigDecimal.ONE
        );
        CreateCandidateResponse candidateResponse = mock(CreateCandidateResponse.class);
        when(candidateResponse.testUrl()).thenReturn("https://praxis.example/test");
        when(candidateResponse.testResultId()).thenReturn("result-1");
        when(integrationAuthService.validateBearerToken(AUTHORIZATION, "recrutei")).thenReturn(CONTEXT);
        when(candidateAttemptService.createOrReuse(any(), eq(CONTEXT))).thenReturn(candidateResponse);

        var response = controller.createCandidateAttempt(AUTHORIZATION, request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().testResultId()).isEqualTo("result-1");
        assertThat(response.getBody().vacancyId()).isEqualTo("vacancy-1");

        ArgumentCaptor<CreateCandidateRequest> requestCaptor = ArgumentCaptor.forClass(CreateCandidateRequest.class);
        verify(candidateAttemptService).createOrReuse(requestCaptor.capture(), eq(CONTEXT));
        assertThat(requestCaptor.getValue().idempotencyScopeId()).isEqualTo("vacancy-1");

        verify(integrationManagementService).recordActivity(
                "empresa-1",
                IntegrationProvider.RECRUTEI,
                "POST /recrutei/test/candidate"
        );
    }

    @Test
    void getTestResultRecordsAuthenticatedEndpointAfterSuccess() {
        CandidateAttempt attempt = mock(CandidateAttempt.class);
        PublishedSimulation simulation = mock(PublishedSimulation.class);
        CandidateAttemptService.AttemptWithSimulation result =
                new CandidateAttemptService.AttemptWithSimulation(attempt, simulation);
        RecruteiTestResultResponse resultResponse = mock(RecruteiTestResultResponse.class);
        when(integrationAuthService.validateBearerToken(AUTHORIZATION, "recrutei")).thenReturn(CONTEXT);
        when(candidateAttemptService.findAttemptResult("result-1", "1", CONTEXT)).thenReturn(result);
        when(recruteiTestResultMapper.toResponse(attempt, simulation)).thenReturn(resultResponse);

        var response = controller.getTestResult(AUTHORIZATION, "result-1", "1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(resultResponse);
        verify(integrationManagementService).recordActivity(
                "empresa-1",
                IntegrationProvider.RECRUTEI,
                "GET /recrutei/test/result/{resultId}"
        );
    }
}
