package br.com.iforce.praxis.shared.integration.service;

import br.com.iforce.praxis.gupy.delivery.service.GupyOutboundUrlValidator;
import br.com.iforce.praxis.shared.integration.IntegrationManagementService;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;
import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttemptEngagementWebhookServiceTest {

    @Mock
    private EmpresaIntegrationRepository integrationRepository;
    @Mock
    private GupyOutboundUrlValidator outboundUrlValidator;
    @Mock
    private HmacSignatureService hmacSignatureService;
    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;
    @Mock
    private IntegrationManagementService integrationManagementService;

    private AttemptEngagementWebhookService service;

    @BeforeEach
    void setUp() {
        when(restClientBuilder.build()).thenReturn(restClient);
        service = new AttemptEngagementWebhookService(
                integrationRepository,
                outboundUrlValidator,
                hmacSignatureService,
                new ObjectMapper(),
                restClientBuilder,
                integrationManagementService
        );
    }

    @Test
    void shouldEnableOnlyExplicitlySelectedCustomApiEvent() {
        EmpresaIntegrationEntity integration = connectedCustomApi(
                """
                {
                  "webhookUrl": "https://cliente.example/webhook",
                  "events": ["RESULT_READY", "ATTEMPT_STARTED"],
                  "secret": "segredo"
                }
                """
        );
        when(integrationRepository.findFirstByEmpresaIdAndProvider("empresa-1", IntegrationProvider.CUSTOM_API))
                .thenReturn(Optional.of(integration));

        assertThat(service.hasActiveWebhook("empresa-1", "ATTEMPT_STARTED")).isTrue();
        assertThat(service.hasActiveWebhook("empresa-1", "ATTEMPT_ABANDONED")).isFalse();

        verify(integrationRepository, times(2))
                .findFirstByEmpresaIdAndProvider("empresa-1", IntegrationProvider.CUSTOM_API);
    }

    @Test
    void shouldNotEnableEventWithoutConnectedCustomApiIntegration() {
        when(integrationRepository.findFirstByEmpresaIdAndProvider("empresa-1", IntegrationProvider.CUSTOM_API))
                .thenReturn(Optional.empty());

        assertThat(service.hasActiveWebhook("empresa-1", "ATTEMPT_STARTED")).isFalse();
    }

    private EmpresaIntegrationEntity connectedCustomApi(String settingsJson) {
        EmpresaIntegrationEntity integration = new EmpresaIntegrationEntity();
        integration.setEmpresaId("empresa-1");
        integration.setProvider(IntegrationProvider.CUSTOM_API);
        integration.setStatus(IntegrationStatus.CONECTADA);
        integration.setSettingsJson(settingsJson);
        return integration;
    }
}
