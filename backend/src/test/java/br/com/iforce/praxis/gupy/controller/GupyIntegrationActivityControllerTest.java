package br.com.iforce.praxis.gupy.controller;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.CreateCandidateResponse;
import br.com.iforce.praxis.gupy.dto.TestResultResponse;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.GupyTestCatalogMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.integration.IntegrationAuthService;
import br.com.iforce.praxis.shared.integration.IntegrationEmpresaContext;
import br.com.iforce.praxis.shared.integration.IntegrationManagementService;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GupyIntegrationActivityControllerTest {

    private static final String AUTHORIZATION = "Bearer token";
    private static final IntegrationEmpresaContext CONTEXT =
            new IntegrationEmpresaContext("empresa-1", "1", "gupy");

    private final IntegrationAuthService integrationAuthService = mock(IntegrationAuthService.class);
    private final SimulationCatalogService simulationCatalogService = mock(SimulationCatalogService.class);
    private final GupyTestCatalogMapper gupyTestCatalogMapper = mock(GupyTestCatalogMapper.class);
    private final CandidateAttemptService candidateAttemptService = mock(CandidateAttemptService.class);
    private final IntegrationManagementService integrationManagementService = mock(IntegrationManagementService.class);
    private final GupyIntegrationController controller = new GupyIntegrationController(
            integrationAuthService,
            simulationCatalogService,
            gupyTestCatalogMapper,
            candidateAttemptService,
            integrationManagementService
    );

    @Test
    void listPublishedTestsRecordsAuthenticatedEndpointAfterSuccess() {
        when(integrationAuthService.validateBearerToken(AUTHORIZATION, "gupy")).thenReturn(CONTEXT);
        when(simulationCatalogService.findPublished("empresa-1", null, 0, 50)).thenReturn(java.util.List.of());
        when(simulationCatalogService.countPublished("empresa-1", null)).thenReturn(0);

        var response = controller.listPublishedTests(AUTHORIZATION, null, 0, 50);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(integrationManagementService).recordActivity(
                "empresa-1",
                IntegrationProvider.GUPY,
                "GET /test"
        );
    }

    @Test
    void createCandidateRecordsAuthenticatedEndpointAfterSuccess() {
        CreateCandidateRequest request = mock(CreateCandidateRequest.class);
        CreateCandidateResponse candidateResponse = mock(CreateCandidateResponse.class);
        when(integrationAuthService.validateBearerToken(AUTHORIZATION, "gupy")).thenReturn(CONTEXT);
        when(candidateAttemptService.createOrReuse(request, CONTEXT)).thenReturn(candidateResponse);

        var response = controller.createCandidateAttempt(AUTHORIZATION, request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isSameAs(candidateResponse);
        verify(integrationManagementService).recordActivity(
                "empresa-1",
                IntegrationProvider.GUPY,
                "POST /test/candidate"
        );
    }

    @Test
    void getTestResultRecordsAuthenticatedEndpointAfterSuccess() {
        TestResultResponse resultResponse = mock(TestResultResponse.class);
        when(integrationAuthService.validateBearerToken(AUTHORIZATION, "gupy")).thenReturn(CONTEXT);
        when(candidateAttemptService.findResult("result-1", "1", CONTEXT)).thenReturn(resultResponse);

        var response = controller.getTestResult(AUTHORIZATION, "result-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isSameAs(resultResponse);
        verify(integrationManagementService).recordActivity(
                "empresa-1",
                IntegrationProvider.GUPY,
                "GET /test/result/{resultId}"
        );
    }
}
