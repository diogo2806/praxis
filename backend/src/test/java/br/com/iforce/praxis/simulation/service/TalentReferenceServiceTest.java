package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.simulation.dto.CandidateReferenceSnapshotDto;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.TalentReferenceSnapshotEntity;
import br.com.iforce.praxis.simulation.persistence.repository.DecisionThresholdPolicyRepository;
import br.com.iforce.praxis.simulation.persistence.repository.NormativeGroupRepository;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import br.com.iforce.praxis.simulation.persistence.repository.TalentReferenceSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TalentReferenceServiceTest {

    @Mock
    private CurrentEmpresaService currentEmpresaService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private SimulationVersionRepository simulationVersionRepository;
    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private NormativeGroupRepository normativeGroupRepository;
    @Mock
    private DecisionThresholdPolicyRepository decisionThresholdPolicyRepository;
    @Mock
    private TalentReferenceSnapshotRepository snapshotRepository;
    @Mock
    private AuditEventService auditEventService;

    private TalentReferenceService service;

    @BeforeEach
    void setUp() {
        service = new TalentReferenceService(
                currentEmpresaService,
                currentUserService,
                simulationVersionRepository,
                candidateAttemptRepository,
                normativeGroupRepository,
                decisionThresholdPolicyRepository,
                snapshotRepository,
                auditEventService,
                new ObjectMapper()
        );
    }

    @Test
    void returnsExistingSnapshotWithoutRecalculatingHistoricalReferences() {
        Instant capturedAt = Instant.parse("2026-06-10T12:00:00Z");
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("attempt-1");

        TalentReferenceSnapshotEntity snapshot = new TalentReferenceSnapshotEntity();
        snapshot.setEmpresaId("empresa-1");
        snapshot.setAttemptId("attempt-1");
        snapshot.setTargetProfileJson("""
                [{
                  "competencyName":"Comunicação",
                  "targetScore":80,
                  "source":"CONFIGURED_TARGET_PROFILE",
                  "warning":"Perfil desejado; não é norma."
                }]
                """);
        snapshot.setNormativeReferenceJson(null);
        snapshot.setDecisionThresholdJson(null);
        snapshot.setCapturedAt(capturedAt);

        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(snapshotRepository.findByEmpresaIdAndAttemptId("empresa-1", "attempt-1"))
                .thenReturn(Optional.of(snapshot));

        CandidateReferenceSnapshotDto response = service.getOrCreateSnapshot(
                attempt,
                new SimulationVersionEntity()
        );

        assertThat(response.capturedAt()).isEqualTo(capturedAt);
        assertThat(response.targetProfile()).hasSize(1);
        assertThat(response.targetProfile().getFirst().competencyName()).isEqualTo("Comunicação");
        assertThat(response.normativeReference()).isNull();
        assertThat(response.decisionThreshold()).isNull();
        verify(snapshotRepository, never()).save(snapshot);
        verifyNoInteractions(
                currentUserService,
                simulationVersionRepository,
                candidateAttemptRepository,
                normativeGroupRepository,
                decisionThresholdPolicyRepository,
                auditEventService
        );
    }
}
