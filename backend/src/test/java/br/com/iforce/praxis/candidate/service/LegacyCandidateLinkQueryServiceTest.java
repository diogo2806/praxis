package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.candidate.dto.CandidateLinkPageResponse;
import br.com.iforce.praxis.candidate.dto.CandidateLinkResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyCandidateLinkQueryServiceTest {

    private final CandidateLinkQueryService queryService = mock(CandidateLinkQueryService.class);
    private final LegacyCandidateLinkQueryService service = new LegacyCandidateLinkQueryService(queryService);

    @Test
    void returnsAllPagesWithoutSilentCut() {
        CandidateLinkResponse first = link("att_1");
        CandidateLinkResponse second = link("att_2");

        when(queryService.search(0, 100, true, null, null, null, null))
                .thenReturn(new CandidateLinkPageResponse(List.of(first), 0, 100, 2, 2));
        when(queryService.search(1, 100, true, null, null, null, null))
                .thenReturn(new CandidateLinkPageResponse(List.of(second), 1, 100, 2, 2));

        List<CandidateLinkResponse> result = service.listAll(true);

        assertThat(result).containsExactly(first, second);
        verify(queryService).search(0, 100, true, null, null, null, null);
        verify(queryService).search(1, 100, true, null, null, null, null);
    }

    @Test
    void returnsEmptyListWhenThereAreNoLinks() {
        when(queryService.search(0, 100, false, null, null, null, null))
                .thenReturn(new CandidateLinkPageResponse(List.of(), 0, 100, 0, 0));

        assertThat(service.listAll(false)).isEmpty();
        verify(queryService).search(0, 100, false, null, null, null, null);
    }

    private CandidateLinkResponse link(String attemptId) {
        return new CandidateLinkResponse(
                attemptId,
                "https://praxis.example.com/candidato/token",
                "Pessoa Candidata",
                "candidate@example.com",
                "sim-1",
                "Avaliação",
                AttemptStatus.NOT_STARTED,
                Instant.parse("2026-07-15T12:00:00Z")
        );
    }
}
