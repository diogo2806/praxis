package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.gupy.dto.GupyHomologationResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.integration.IntegrationTokenEntity;
import br.com.iforce.praxis.shared.integration.IntegrationTokenRepository;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;
import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;
import br.com.iforce.praxis.shared.outbox.persistence.repository.OutboxEventRepository;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GupyHomologationServiceTest {

    @Mock
    private CurrentEmpresaService currentEmpresaService;
    @Mock
    private IntegrationTokenRepository integrationTokenRepository;
    @Mock
    private EmpresaIntegrationRepository empresaIntegrationRepository;
    @Mock
    private SimulationVersionRepository simulationVersionRepository;
    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private OutboxEventRepository outboxEventRepository;

    private GupyHomologationService service;

    @BeforeEach
    void setUp() {
        service = serviceWithBaseUrl("https://praxis.example.com");
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("tenant-1");
    }

    @Test
    void blocksExternalValidationWhenInternalRequirementsAreMissing() {
        service = serviceWithBaseUrl("http://localhost:8080");
        when(integrationTokenRepository.findFirstByEmpresaIdAndProvider("tenant-1", "gupy"))
                .thenReturn(Optional.empty());
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider("tenant-1", IntegrationProvider.GUPY))
                .thenReturn(Optional.empty());
        when(simulationVersionRepository.countBySimulationEmpresaIdAndStatus(
                "tenant-1",
                SimulationVersionStatus.PUBLISHED
        )).thenReturn(0L);
        stubEmptyEvidence();

        GupyHomologationResponse response = service.getStatus();

        assertThat(response.status()).isEqualTo("BLOCKED");
        assertThat(response.readinessPercent()).isZero();
        assertThat(response.checks())
                .filteredOn(check -> "BLOCKER".equals(check.status()))
                .extracting(GupyHomologationResponse.Check::code)
                .containsExactlyInAnyOrder("PUBLIC_HTTPS", "ACCESS_TOKEN", "PUBLISHED_CATALOG");
    }

    @Test
    void exposesCompleteEvidenceWithoutClaimingExternalApproval() {
        IntegrationTokenEntity token = new IntegrationTokenEntity();
        EmpresaIntegrationEntity integration = new EmpresaIntegrationEntity();
        Instant evidenceAt = Instant.parse("2026-07-17T20:00:00Z");
        integration.setStatus(IntegrationStatus.CONECTADA);
        integration.setLastSyncAt(evidenceAt);

        when(integrationTokenRepository.findFirstByEmpresaIdAndProvider("tenant-1", "gupy"))
                .thenReturn(Optional.of(token));
        when(empresaIntegrationRepository.findFirstByEmpresaIdAndProvider("tenant-1", IntegrationProvider.GUPY))
                .thenReturn(Optional.of(integration));
        when(simulationVersionRepository.countBySimulationEmpresaIdAndStatus(
                "tenant-1",
                SimulationVersionStatus.PUBLISHED
        )).thenReturn(2L);
        when(candidateAttemptRepository.countByEmpresaIdAndCallbackUrlIsNotNull("tenant-1")).thenReturn(1L);
        when(candidateAttemptRepository.countByEmpresaIdAndCallbackUrlIsNotNullAndStatus(
                "tenant-1",
                AttemptStatus.COMPLETED
        )).thenReturn(1L);
        when(candidateAttemptRepository.countByEmpresaIdAndCallbackUrlIsNotNullAndResultWebhookUrlIsNotNull("tenant-1"))
                .thenReturn(1L);
        when(candidateAttemptRepository.findLastGupyAttemptCreatedAt("tenant-1"))
                .thenReturn(Optional.of(evidenceAt));
        when(outboxEventRepository.countGupyResultDeliveriesByStatus("tenant-1", "SENT")).thenReturn(1L);
        when(outboxEventRepository.countGupyResultDeliveriesByStatus("tenant-1", "DLQ")).thenReturn(0L);

        GupyHomologationResponse response = service.getStatus();

        assertThat(response.status()).isEqualTo("EVIDENCE_READY");
        assertThat(response.readinessPercent()).isEqualTo(100);
        assertThat(response.externalApprovalRequired()).isTrue();
        assertThat(response.metrics().sentResultWebhooks()).isEqualTo(1L);
        assertThat(response.checks())
                .filteredOn(check -> "GUPY_APPROVAL".equals(check.code()))
                .singleElement()
                .satisfies(check -> assertThat(check.status()).isEqualTo("PENDING"));
    }

    private void stubEmptyEvidence() {
        when(candidateAttemptRepository.countByEmpresaIdAndCallbackUrlIsNotNull("tenant-1")).thenReturn(0L);
        when(candidateAttemptRepository.countByEmpresaIdAndCallbackUrlIsNotNullAndStatus(
                "tenant-1",
                AttemptStatus.COMPLETED
        )).thenReturn(0L);
        when(candidateAttemptRepository.countByEmpresaIdAndCallbackUrlIsNotNullAndResultWebhookUrlIsNotNull("tenant-1"))
                .thenReturn(0L);
        when(candidateAttemptRepository.findLastGupyAttemptCreatedAt("tenant-1")).thenReturn(Optional.empty());
        when(outboxEventRepository.countGupyResultDeliveriesByStatus("tenant-1", "SENT")).thenReturn(0L);
        when(outboxEventRepository.countGupyResultDeliveriesByStatus("tenant-1", "DLQ")).thenReturn(0L);
    }

    private GupyHomologationService serviceWithBaseUrl(String baseUrl) {
        PraxisProperties properties = new PraxisProperties(
                baseUrl,
                baseUrl,
                168,
                24,
                720,
                70,
                15,
                0.001
        );
        return new GupyHomologationService(
                currentEmpresaService,
                integrationTokenRepository,
                empresaIntegrationRepository,
                simulationVersionRepository,
                candidateAttemptRepository,
                outboxEventRepository,
                properties
        );
    }
}
