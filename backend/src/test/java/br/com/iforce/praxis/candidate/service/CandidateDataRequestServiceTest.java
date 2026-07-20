package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.candidate.dto.DataSubjectRequest;
import br.com.iforce.praxis.candidate.dto.DataSubjectRequestType;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.privacy.persistence.entity.DataSubjectRequestEntity;
import br.com.iforce.praxis.shared.privacy.persistence.repository.DataSubjectRequestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collection;
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
class CandidateDataRequestServiceTest {

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private AuditEventService auditEventService;
    @Mock
    private DataSubjectRequestRepository dataSubjectRequestRepository;
    @Mock
    private CandidateAttemptTokenResolver tokenResolver;

    private CandidateDataRequestService service;

    @BeforeEach
    void setUp() {
        service = new CandidateDataRequestService(
                candidateAttemptRepository,
                auditEventService,
                new ObjectMapper(),
                dataSubjectRequestRepository,
                tokenResolver,
                15
        );
    }

    @Test
    void persistsDataSubjectRequestWithoutPuttingFreeTextInAuditMetadata() {
        CandidateAttemptEntity attempt = attempt("att_1");
        when(tokenResolver.resolve("token_1"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-9", "att_1"));
        when(candidateAttemptRepository.findById("att_1")).thenReturn(Optional.of(attempt));
        when(dataSubjectRequestRepository.existsByAttemptIdAndRequestTypeAndStatusIn(
                eq("att_1"), eq(DataSubjectRequestType.ANONYMIZATION_DELETION), any(Collection.class)))
                .thenReturn(false);

        service.register("token_1", new DataSubjectRequest(
                DataSubjectRequestType.ANONYMIZATION_DELETION,
                "titular@example.com",
                "Quero excluir meus dados."
        ));

        ArgumentCaptor<DataSubjectRequestEntity> requestCaptor =
                ArgumentCaptor.forClass(DataSubjectRequestEntity.class);
        verify(dataSubjectRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getContact()).isEqualTo("titular@example.com");
        assertThat(requestCaptor.getValue().getDetails()).isEqualTo("Quero excluir meus dados.");
        assertThat(requestCaptor.getValue().getDueAt()).isAfter(requestCaptor.getValue().getRequestedAt());

        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("empresa-9"), eq("att_1"), eq(AuditEventType.DATA_SUBJECT_REQUEST),
                anyString(), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue())
                .contains("\"source\":\"candidate\"")
                .contains("\"requestType\":\"anonymizationDeletion\"")
                .doesNotContain("titular@example.com")
                .doesNotContain("Quero excluir meus dados.");
    }

    @Test
    void rejectsDuplicateOpenRequest() {
        CandidateAttemptEntity attempt = attempt("att_2");
        when(tokenResolver.resolve("token_2"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-9", "att_2"));
        when(candidateAttemptRepository.findById("att_2")).thenReturn(Optional.of(attempt));
        when(dataSubjectRequestRepository.existsByAttemptIdAndRequestTypeAndStatusIn(
                eq("att_2"), eq(DataSubjectRequestType.CONFIRMATION_ACCESS), any(Collection.class)))
                .thenReturn(true);

        assertThatThrownBy(() -> service.register("token_2", new DataSubjectRequest(
                DataSubjectRequestType.CONFIRMATION_ACCESS, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
    }

    @Test
    void rejectsMissingRequestType() {
        assertThatThrownBy(() -> service.register("token_3", new DataSubjectRequest(null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        verifyNoInteractions(candidateAttemptRepository, auditEventService, dataSubjectRequestRepository, tokenResolver);
    }

    @Test
    void rejectsUnknownAttempt() {
        when(tokenResolver.resolve("ghost-token"))
                .thenReturn(new CandidateAttemptTokenResolver.ResolvedAttemptToken("empresa-9", "ghost"));
        when(candidateAttemptRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("ghost-token",
                new DataSubjectRequest(DataSubjectRequestType.PORTABILITY, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verifyNoInteractions(auditEventService, dataSubjectRequestRepository);
    }

    private CandidateAttemptEntity attempt(String id) {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId(id);
        attempt.setEmpresaId("empresa-9");
        return attempt;
    }
}
