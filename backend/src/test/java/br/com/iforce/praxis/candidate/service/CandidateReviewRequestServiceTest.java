package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.candidate.dto.ReviewRequest;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    private CandidateReviewRequestService service;

    @BeforeEach
    void setUp() {
        service = new CandidateReviewRequestService(
                candidateAttemptRepository, auditEventService, new ObjectMapper());
    }

    @Test
    void recordsReviewRequestOnTheAttemptTenantTrail() {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att_1");
        attempt.setTenantId("tenant-9");
        when(candidateAttemptRepository.findById("att_1")).thenReturn(Optional.of(attempt));

        service.register("att_1", new ReviewRequest("Discordo do resultado."));

        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("tenant-9"),
                eq("att_1"),
                eq(AuditEventType.REVIEW_REQUESTED),
                anyString(),
                metadataCaptor.capture()
        );
        assertThat(metadataCaptor.getValue())
                .contains("\"source\":\"candidate\"")
                .contains("Discordo do resultado.")
                .contains("requestedAt");
    }

    @Test
    void toleratesMissingBody() {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att_2");
        attempt.setTenantId("tenant-9");
        when(candidateAttemptRepository.findById("att_2")).thenReturn(Optional.of(attempt));

        service.register("att_2", null);

        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("tenant-9"), eq("att_2"), eq(AuditEventType.REVIEW_REQUESTED), anyString(), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue()).contains("\"reason\":null");
    }

    @Test
    void rejectsUnknownAttempt() {
        when(candidateAttemptRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("ghost", new ReviewRequest(null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verifyNoInteractions(auditEventService);
    }
}
