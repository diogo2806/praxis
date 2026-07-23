package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.simulation.dto.NormativeGroupRequest;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NormativeGroupActivationGuardTest {

    @Mock
    private CurrentEmpresaService currentEmpresaService;
    @Mock
    private SimulationVersionRepository simulationVersionRepository;
    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    private NormativeGroupActivationGuard guard;

    @BeforeEach
    void setUp() {
        guard = new NormativeGroupActivationGuard(
                currentEmpresaService,
                simulationVersionRepository,
                candidateAttemptRepository
        );
    }

    @Test
    void rejectsActivationBelowMinimumSampleBeforeCurrentReferenceCanBeArchived() {
        Instant start = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS);
        NormativeGroupRequest request = request(start, end, 30, true);
        SimulationVersionEntity version = version(11L);

        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                "empresa-1",
                "sim-1",
                2
        )).thenReturn(Optional.of(version));
        when(candidateAttemptRepository.findByEmpresaIdAndSimulationVersionIdAndStatus(
                "empresa-1",
                11L,
                AttemptStatus.COMPLETED
        )).thenReturn(completedAttempts(29));

        assertThatThrownBy(() -> guard.assertEligibleForActivation("sim-1", 2, request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void acceptsActivationWithMinimumSampleAndComparablePaths() {
        Instant start = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS);
        NormativeGroupRequest request = request(start, end, 30, true);
        SimulationVersionEntity version = version(11L);

        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(simulationVersionRepository.findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                "empresa-1",
                "sim-1",
                2
        )).thenReturn(Optional.of(version));
        when(candidateAttemptRepository.findByEmpresaIdAndSimulationVersionIdAndStatus(
                "empresa-1",
                11L,
                AttemptStatus.COMPLETED
        )).thenReturn(completedAttempts(30));

        assertThatCode(() -> guard.assertEligibleForActivation("sim-1", 2, request))
                .doesNotThrowAnyException();
    }

    @Test
    void draftConfigurationDoesNotQueryOrReplaceActiveReference() {
        Instant start = Instant.now().minus(90, ChronoUnit.DAYS);
        Instant end = Instant.now().plus(1, ChronoUnit.DAYS);
        NormativeGroupRequest request = request(start, end, 30, false);

        assertThatCode(() -> guard.assertEligibleForActivation("sim-1", 2, request))
                .doesNotThrowAnyException();
        verifyNoInteractions(
                currentEmpresaService,
                simulationVersionRepository,
                candidateAttemptRepository
        );
    }

    private NormativeGroupRequest request(
            Instant start,
            Instant end,
            int minimumSample,
            boolean activate
    ) {
        return new NormativeGroupRequest(
                "Atendentes sênior",
                "Atendente",
                "Sênior",
                123L,
                "Pessoas candidatas da mesma vaga, versão e período.",
                start,
                end,
                minimumSample,
                true,
                activate
        );
    }

    private SimulationVersionEntity version(Long id) {
        SimulationVersionEntity version = new SimulationVersionEntity();
        version.setId(id);
        return version;
    }

    private List<CandidateAttemptEntity> completedAttempts(int count) {
        Instant finishedAt = Instant.now().minus(10, ChronoUnit.DAYS);
        return IntStream.range(0, count)
                .mapToObj(index -> {
                    CandidateAttemptEntity attempt = new CandidateAttemptEntity();
                    attempt.setId("attempt-" + index);
                    attempt.setStatus(AttemptStatus.COMPLETED);
                    attempt.setFinishedAt(finishedAt);
                    attempt.setGupyJobId(123L);
                    attempt.setScore(60 + index % 30);
                    return attempt;
                })
                .toList();
    }
}
