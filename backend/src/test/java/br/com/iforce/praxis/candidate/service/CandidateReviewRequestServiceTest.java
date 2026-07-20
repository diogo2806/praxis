package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.candidate.dto.ReviewRequest;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.privacy.persistence.entity.HumanReviewRequestEntity;
import br.com.iforce.praxis.shared.privacy.persistence.repository.HumanReviewRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateReviewRequestServiceTest {

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private HumanReviewRequestRepository humanReviewRequestRepository;
    @Mock
    private CandidateAttemptTokenResolver tokenResolver;

    private CandidateReviewRequestService service;

    @BeforeEach
    void setUp() {
        service = new CandidateReviewRequestService(
                candidateAttemptRepository,
                auditEventService,
                new ObjectMapper(),
                humanReviewRequestRepository,
                tokenResolver,
                5
        );
    }

    @Test
    void persistsReviewRequestAndMarksAttemptForHumanReview() {
        CandidateAttemptEntity attempt = attempt("att_1");
        when(tokenResolver.resolve("token_1"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-9", "att_1"));
        when(candidateAttemptRepository.findById("att_1")).thenReturn(Optional.of(attempt));
        when(humanReviewRequestRepository.findFirstByAttemptIdAndStatusInOrderByRequestedAtDesc(
                eq("att_1"), any(List.class))).thenReturn(Optional.empty());

        service.register("token_1", new ReviewRequest("Discordo do resultado."));

        ArgumentCaptor<HumanReviewRequestEntity> entityCaptor =
                ArgumentCaptor.forClass(HumanReviewRequestEntity.class);
        verify(humanReviewRequestRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getReason()).isEqualTo("Discordo do resultado.");
        assertThat(entityCaptor.getValue().getDueAt()).isAfter(entityCaptor.getValue().getRequestedAt());
        assertThat(attempt.isHumanReviewRequired()).isTrue();
        verify(candidateAttemptRepository).save(attempt);
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("empresa-9"), eq("att_1"), eq(AuditEventType.REVIEW_REQUESTED),
                anyString(), anyString());
    }

    @Test
    void reusesExistingOpenReviewRequest() {
        CandidateAttemptEntity attempt = attempt("att_2");
        HumanReviewRequestEntity existing = new HumanReviewRequestEntity();
        existing.setId("review-existing");
        when(tokenResolver.resolve("token_2"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-9", "att_2"));
        when(candidateAttemptRepository.findById("att_2")).thenReturn(Optional.of(attempt));
        when(humanReviewRequestRepository.findFirstByAttemptIdAndStatusInOrderByRequestedAtDesc(
                eq("att_2"), any(List.class))).thenReturn(Optional.of(existing));

        assertThat(service.register("token_2", null)).isEqualTo("review-existing");
        verifyNoInteractions(auditEventService);
    }

    @Test
    void rejectsUnknownAttempt() {
        when(tokenResolver.resolve("ghost-token"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-9", "ghost"));
        when(candidateAttemptRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("ghost-token", new ReviewRequest(null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verifyNoInteractions(auditEventService);
    }

    private CandidateAttemptEntity attempt(String id) {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId(id);
        attempt.setEmpresaId("empresa-9");
        return attempt;
    }
}
