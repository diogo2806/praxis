package br.com.iforce.praxis.gupy.controller;

import br.com.iforce.praxis.gupy.dto.TestItemsResponse;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.gupy.service.GupyTestCatalogMapper;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.shared.integration.IntegrationAuthService;
import br.com.iforce.praxis.shared.integration.IntegrationEmpresaContext;
import br.com.iforce.praxis.shared.integration.IntegrationManagementService;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GupyPaginationContractTest {

    @Mock
    private IntegrationAuthService integrationAuthService;
    @Mock
    private SimulationCatalogService simulationCatalogService;
    @Mock
    private GupyTestCatalogMapper gupyTestCatalogMapper;
    @Mock
    private CandidateAttemptService candidateAttemptService;
    @Mock
    private IntegrationManagementService integrationManagementService;

    @Test
    void limitZeroReturnsOnlyTotalWithoutFetchingCatalogItems() {
        IntegrationEmpresaContext context = new IntegrationEmpresaContext("empresa-1", "1", "gupy");
        when(integrationAuthService.validateBearerToken("Bearer token", "gupy")).thenReturn(context);
        when(simulationCatalogService.countPublished("empresa-1", null)).thenReturn(7);

        GupyIntegrationController controller = new GupyIntegrationController(
                integrationAuthService,
                simulationCatalogService,
                gupyTestCatalogMapper,
                candidateAttemptService,
                integrationManagementService
        );

        ResponseEntity<TestItemsResponse> response = controller.listPublishedTests(
                "Bearer token",
                null,
                0,
                0
        );

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().limit()).isZero();
        assertThat(response.getBody().offset()).isZero();
        assertThat(response.getBody().total_tests()).isEqualTo(7);
        assertThat(response.getBody().payload()).isEmpty();
        verify(simulationCatalogService, never()).findPublished("empresa-1", null, 0, 0);
        verify(integrationManagementService).recordActivity(
                "empresa-1",
                IntegrationProvider.GUPY,
                "GET /test"
        );
    }
}
