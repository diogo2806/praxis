package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.audit.persistence.repository.AuditEventRepository;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
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
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private CurrentUserService currentUserService;
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
    @Mock
    private AuditEventRepository auditEventRepository;
    @Mock
    private AuditEventService auditEventService;

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
        EmpresaIntegrationEntity integration = completeIntegration(false, false);
        stubCompleteAutomaticEvidence(integration);

        GupyHomologationResponse response = service.getStatus();

        assertThat(response.status()).isEqualTo("EVIDENCE_READY");
        assertThat(response.readinessPercent()).isEqualTo(100);
        assertThat(response.externalApprovalRequired()).isTrue();
        assertThat(response.metrics().sentResultWebhooks()).isEqualTo(1L);
        assertThat(response.metrics().resultEndpointQueries()).isEqualTo(1L);
        assertThat(response.metrics().validPercentageResults()).isEqualTo(1L);
        assertThat(response.checks())
                .filteredOn(check -> "GUPY_APPROVAL".equals(check.code()) || "CLIENT_APPROVAL".equals(check.code()))
                .allSatisfy(check -> assertThat(check.status()).isEqualTo("PENDING"));
    }

    @Test
    void marksHomologatedOnlyAfterBothFormalApprovals() {
        EmpresaIntegrationEntity integration = completeIntegration(true, true);
        stubCompleteAutomaticEvidence(integration);

        GupyHomologationResponse response = service.getStatus();

        assertThat(response.status()).isEqualTo("HOMOLOGATED");
        assertThat(response.externalApprovalRequired()).isFalse();
        assertThat(response.checks())
                .filteredOn(check -> "GUPY_APPROVAL".equals(check.code()) || "CLIENT_APPROVAL".equals(check.code()))
                .allSatisfy(check -> assertThat(check.status()).isEqualTo("OK"));
    }

    @Test
    void keepsValidationPendingWhenResultWasNotQueried() {
        EmpresaIntegrationEntity integration = completeIntegration(false, false);
        stubCompleteAutomaticEvidence(integration);
        when(auditEventRepository.countIntegrationEndpointEvidence(
                "tenant-1",
                "GUPY",
                "GET /test/result/{resultId}"
        )).thenReturn(0L);

        GupyHomologationResponse response = service.getStatus();

        assertThat(response.status()).isEqualTo("READY_FOR_EXTERNAL_VALIDATION");
        assertThat(response.checks())
                .filteredOn(check -> "RESULT_ENDPOINT_QUERIED".equals(check.code()))
                .singleElement()
                .satisfies(check -> assertThat(check.status()).isEqualTo("PENDING"));
    }

    private EmpresaIntegrationEntity completeIntegration(boolean gupyApproved, boolean clientApproved) {
        EmpresaIntegrationEntity integration = new EmpresaIntegrationEntity();
        Instant evidenceAt = Instant.parse("2026-07-17T20:00:00Z");
        integration.setStatus(IntegrationStatus.CONECTADA);
        integration.setLastSyncAt(evidenceAt);
        integration.setSettingsJson("""
                {
                  "homologationEvidence": {
                    "callbackConfirmed": true,
                    "callbackConfirmedAt": "2026-07-17T20:00:00Z",
                    "callbackConfirmedBy": "admin-1",
                    "resultPagesConfirmed": true,
                    "resultPagesConfirmedAt": "2026-07-17T20:10:00Z",
                    "resultPagesConfirmedBy": "admin-1",
                    "gupyApprovalConfirmed": %s,
                    "gupyApprovalConfirmedAt": "2026-07-17T21:00:00Z",
                    "gupyApprovalConfirmedBy": "admin-1",
                    "clientApprovalConfirmed": %s,
                    "clientApprovalConfirmedAt": "2026-07-17T21:10:00Z",
                    "clientApprovalConfirmedBy": "admin-1",
                    "notes": "Vaga de homologação validada."
                  }
                }
                """.formatted(gupyApproved, clientApproved));
        return integration;
    }

    private void stubCompleteAutomaticEvidence(EmpresaIntegrationEntity integration) {
        IntegrationTokenEntity token = new IntegrationTokenEntity();
        Instant evidenceAt = Instant.parse("2026-07-17T20:00:00Z");
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
        when(candidateAttemptRepository.countGupyCompletedAttemptsWithValidPercentage(
                "tenant-1",
                AttemptStatus.COMPLETED
        )).thenReturn(1L);
        when(candidateAttemptRepository.findLastGupyAttemptCreatedAt("tenant-1"))
                .thenReturn(Optional.of(evidenceAt));
        when(outboxEventRepository.countGupyResultDeliveriesByStatus("tenant-1", "SENT")).thenReturn(1L);
        when(outboxEventRepository.countGupyResultDeliveriesByStatus("tenant-1", "DLQ")).thenReturn(0L);
        when(auditEventRepository.countIntegrationEndpointEvidence(
                "tenant-1",
                "GUPY",
                "GET /test/result/{resultId}"
        )).thenReturn(1L);
    }

    private void stubEmptyEvidence() {
        when(candidateAttemptRepository.countByEmpresaIdAndCallbackUrlIsNotNull("tenant-1")).thenReturn(0L);
        when(candidateAttemptRepository.countByEmpresaIdAndCallbackUrlIsNotNullAndStatus(
                "tenant-1",
                AttemptStatus.COMPLETED
        )).thenReturn(0L);
        when(candidateAttemptRepository.countByEmpresaIdAndCallbackUrlIsNotNullAndResultWebhookUrlIsNotNull("tenant-1"))
                .thenReturn(0L);
        when(candidateAttemptRepository.countGupyCompletedAttemptsWithValidPercentage(
                "tenant-1",
                AttemptStatus.COMPLETED
        )).thenReturn(0L);
        when(candidateAttemptRepository.findLastGupyAttemptCreatedAt("tenant-1")).thenReturn(Optional.empty());
        when(outboxEventRepository.countGupyResultDeliveriesByStatus("tenant-1", "SENT")).thenReturn(0L);
        when(outboxEventRepository.countGupyResultDeliveriesByStatus("tenant-1", "DLQ")).thenReturn(0L);
        when(auditEventRepository.countIntegrationEndpointEvidence(
                "tenant-1",
                "GUPY",
                "GET /test/result/{resultId}"
        )).thenReturn(0L);
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
                currentUserService,
                integrationTokenRepository,
                empresaIntegrationRepository,
                simulationVersionRepository,
                candidateAttemptRepository,
                outboxEventRepository,
                auditEventRepository,
                auditEventService,
                properties,
                new ObjectMapper()
        );
    }
}
