package br.com.iforce.praxis.dashboard.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptNodeServeEntity;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.model.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAnalyticsServiceTest {

    @Mock
    private CurrentEmpresaService currentEmpresaService;
    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    private DashboardAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new DashboardAnalyticsService(currentEmpresaService, candidateAttemptRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAnalyticsBuildsFunnelRatesActivityAndMediaQualityWithoutMixingVersions() {
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        CandidateAttemptEntity created = attempt("created", AttemptStatus.IN_PROGRESS, yesterday, null, null);
        CandidateAttemptEntity completed = attempt(
                "completed",
                AttemptStatus.COMPLETED,
                yesterday.minusSeconds(60),
                yesterday,
                80
        );
        CandidateAttemptEntity abandoned = attempt("abandoned", AttemptStatus.ABANDONED, yesterday, yesterday, null);
        addMedia(created, "node-1", MediaType.VIDEO, "video-v1", null);
        addMedia(completed, "node-1", MediaType.VIDEO, "video-v1", "A");
        addMedia(abandoned, "node-2", MediaType.VIDEO, "video-v2", "B");

        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(candidateAttemptRepository.count(any(Specification.class))).thenReturn(10L, 8L);
        when(candidateAttemptRepository.countByEmpresaIdAndStatus("empresa-1", AttemptStatus.NOT_STARTED)).thenReturn(2L);
        when(candidateAttemptRepository.countByEmpresaIdAndStatus("empresa-1", AttemptStatus.IN_PROGRESS)).thenReturn(2L);
        when(candidateAttemptRepository.countByEmpresaIdAndStatus("empresa-1", AttemptStatus.COMPLETED)).thenReturn(4L);
        when(candidateAttemptRepository.countByEmpresaIdAndStatus("empresa-1", AttemptStatus.ABANDONED)).thenReturn(1L);
        when(candidateAttemptRepository.countByEmpresaIdAndStatus("empresa-1", AttemptStatus.EXPIRED)).thenReturn(1L);
        when(candidateAttemptRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(created), List.of(completed, abandoned));

        DashboardAnalyticsResponse response = service.getAnalytics();

        assertThat(response.periodDays()).isEqualTo(30);
        assertThat(response.activity()).hasSize(30);
        assertThat(response.participations().total()).isEqualTo(10);
        assertThat(response.participations().started()).isEqualTo(8);
        assertThat(response.participations().completionRatePercent()).isEqualTo(66.7);
        assertThat(response.participations().dropOffRatePercent()).isEqualTo(33.3);
        assertThat(response.participations().averageScoreLast30Days()).isEqualTo(80.0);
        assertThat(response.mediaQualityComparisons()).hasSize(2);
        DashboardAnalyticsResponse.MediaQualityComparison videoV1 = response.mediaQualityComparisons().getFirst();
        assertThat(videoV1.mediaVersion()).isEqualTo("video-v1");
        assertThat(videoV1.sampleSize()).isEqualTo(2);
        assertThat(videoV1.completed()).isEqualTo(1);
        assertThat(videoV1.completionRatePercent()).isEqualTo(50.0);
        assertThat(videoV1.averageDurationSeconds()).isEqualTo(60.0);
        assertThat(videoV1.responseDistribution()).singleElement().satisfies(item -> {
            assertThat(item.responseId()).isEqualTo("A");
            assertThat(item.percentage()).isEqualTo(100.0);
        });
    }

    private static CandidateAttemptEntity attempt(
            String id,
            AttemptStatus status,
            Instant createdAt,
            Instant finishedAt,
            Integer score
    ) {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId(id);
        attempt.setEmpresaId("empresa-1");
        attempt.setStatus(status);
        attempt.setCreatedAt(createdAt);
        attempt.setStartedAt(createdAt);
        attempt.setFinishedAt(finishedAt);
        attempt.setScore(score);
        return attempt;
    }

    private static void addMedia(
            CandidateAttemptEntity attempt,
            String nodeId,
            MediaType mediaType,
            String mediaVersion,
            String optionId
    ) {
        AttemptNodeServeEntity serve = new AttemptNodeServeEntity();
        serve.setCandidateAttempt(attempt);
        serve.setNodeId(nodeId);
        serve.setServedAt(attempt.getCreatedAt());
        serve.setMediaType(mediaType);
        serve.setMediaVersion(mediaVersion);
        attempt.getNodeServes().add(serve);

        if (optionId != null) {
            AttemptAnswerEntity answer = new AttemptAnswerEntity();
            answer.setCandidateAttempt(attempt);
            answer.setNodeId(nodeId);
            answer.setOptionId(optionId);
            answer.setTimedOut(false);
            answer.setAnsweredAt(attempt.getCreatedAt());
            answer.setReceivedAt(attempt.getCreatedAt());
            attempt.getAnswers().add(answer);
        }
    }
}
