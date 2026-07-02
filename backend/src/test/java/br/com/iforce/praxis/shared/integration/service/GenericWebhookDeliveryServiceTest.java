package br.com.iforce.praxis.shared.integration.service;

import br.com.iforce.praxis.config.PraxisProperties;

import br.com.iforce.praxis.gupy.model.ResultDecision;

import br.com.iforce.praxis.gupy.model.ResultTier;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.entity.ResultItemEntity;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;

import br.com.iforce.praxis.shared.integration.IntegrationManagementService;

import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;

import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;

import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;

import org.springframework.web.client.RestClient;


import java.util.List;

import java.util.Map;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.when;


class GenericWebhookDeliveryServiceTest {

    private final EmpresaIntegrationRepository repository = mock(EmpresaIntegrationRepository.class);

    private final IntegrationManagementService integrationManagementService = mock(IntegrationManagementService.class);

    private final GenericWebhookDeliveryService service = new GenericWebhookDeliveryService(
            repository,
            null,
            null,
            new HmacSignatureService(),
            null,
            new PraxisProperties("https://praxis.example.com", 168, 24, 70, 15, 0.001),
            new ObjectMapper(),
            RestClient.builder(),
            integrationManagementService
    );

    @Test
    void buildsResultReadyPayloadFromAttempt() {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att_4f7c");
        attempt.setSimulationId("sim-atendimento");
        attempt.setScore(78);
        attempt.setDecision(ResultDecision.RECOMMEND_INTERVIEW);
        attempt.getResultItems().add(resultItem(attempt, "Resolução de Conflitos", 82));

        Map<String, Object> payload = service.buildResultReadyPayload("acme", attempt);

        assertThat(payload).containsEntry("event", "RESULT_READY");
        assertThat(payload).containsEntry("tenantId", "acme");
        assertThat(payload).containsEntry("attemptId", "att_4f7c");
        assertThat(payload).containsEntry("simulationId", "sim-atendimento");
        assertThat(payload).containsEntry("score", 78);
        assertThat(payload).containsEntry("decision", "RECOMMEND_INTERVIEW");
        assertThat(payload).containsEntry("resultUrl", "https://praxis.example.com/results/att_4f7c");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> competencies = (List<Map<String, Object>>) payload.get("competencies");
        assertThat(competencies).hasSize(1);
        assertThat(competencies.get(0)).containsEntry("name", "Resolução de Conflitos");
        assertThat(competencies.get(0)).containsEntry("score", 82);
    }

    @Test
    void detectsActiveResultWebhook() {
        when(repository.findFirstByEmpresaIdAndProvider("acme", IntegrationProvider.CUSTOM_API))
                .thenReturn(Optional.of(integration(
                        IntegrationStatus.CONECTADA,
                        "{\"webhookUrl\":\"https://meu-ats.com/hook\",\"events\":[\"RESULT_READY\"],\"secret\":\"whsec_x\"}"
                )));

        assertThat(service.hasActiveResultWebhook("acme")).isTrue();
    }

    @Test
    void ignoresWebhookWhenDisabled() {
        when(repository.findFirstByEmpresaIdAndProvider("acme", IntegrationProvider.CUSTOM_API))
                .thenReturn(Optional.of(integration(
                        IntegrationStatus.DESATIVADA,
                        "{\"webhookUrl\":\"https://meu-ats.com/hook\",\"events\":[\"RESULT_READY\"],\"secret\":\"whsec_x\"}"
                )));

        assertThat(service.hasActiveResultWebhook("acme")).isFalse();
    }

    @Test
    void ignoresWebhookWhenEventNotSubscribed() {
        when(repository.findFirstByEmpresaIdAndProvider("acme", IntegrationProvider.CUSTOM_API))
                .thenReturn(Optional.of(integration(
                        IntegrationStatus.CONECTADA,
                        "{\"webhookUrl\":\"https://meu-ats.com/hook\",\"events\":[\"ATTEMPT_STARTED\"],\"secret\":\"whsec_x\"}"
                )));

        assertThat(service.hasActiveResultWebhook("acme")).isFalse();
    }

    private EmpresaIntegrationEntity integration(IntegrationStatus status, String settingsJson) {
        EmpresaIntegrationEntity entity = new EmpresaIntegrationEntity();
        entity.setProvider(IntegrationProvider.CUSTOM_API);
        entity.setStatus(status);
        entity.setSettingsJson(settingsJson);
        return entity;
    }

    private ResultItemEntity resultItem(CandidateAttemptEntity attempt, String name, int score) {
        ResultItemEntity item = new ResultItemEntity();
        item.setCandidateAttempt(attempt);
        item.setName(name);
        item.setScore(score);
        item.setTier(ResultTier.MAJOR);
        return item;
    }
}
