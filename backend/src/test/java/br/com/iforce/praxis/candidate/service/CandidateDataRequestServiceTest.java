package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.candidate.dto.DataSubjectRequest;

import br.com.iforce.praxis.candidate.dto.DataSubjectRequestType;

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
class CandidateDataRequestServiceTest {

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    @Mock
    private AuditEventService auditEventService;

    private CandidateDataRequestService service;

    @BeforeEach
    void setUp() {
        service = new CandidateDataRequestService(
                candidateAttemptRepository, auditEventService, new ObjectMapper());
    }

    @Test
    void recordsDataSubjectRequestOnTheAttemptEmpresaTrail() {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att_1");
        attempt.setEmpresaId("empresa-9");
        when(candidateAttemptRepository.findById("att_1")).thenReturn(Optional.of(attempt));

        service.register("att_1", new DataSubjectRequest(
                DataSubjectRequestType.ANONYMIZATION_DELETION,
                "titular@example.com",
                "Quero excluir meus dados."));

        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("empresa-9"),
                eq("att_1"),
                eq(AuditEventType.DATA_SUBJECT_REQUEST),
                anyString(),
                metadataCaptor.capture()
        );
        assertThat(metadataCaptor.getValue())
                .contains("\"source\":\"candidate\"")
                .contains("\"requestType\":\"anonymizationDeletion\"")
                .contains("titular@example.com")
                .contains("Quero excluir meus dados.")
                .contains("requestedAt");
    }

    @Test
    void toleratesOptionalContactAndDetails() {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att_2");
        attempt.setEmpresaId("empresa-9");
        when(candidateAttemptRepository.findById("att_2")).thenReturn(Optional.of(attempt));

        service.register("att_2", new DataSubjectRequest(
                DataSubjectRequestType.CONFIRMATION_ACCESS, null, "   "));

        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("empresa-9"), eq("att_2"), eq(AuditEventType.DATA_SUBJECT_REQUEST), anyString(), metadataCaptor.capture());
        assertThat(metadataCaptor.getValue())
                .contains("\"contact\":null")
                .contains("\"details\":null");
    }

    @Test
    void rejectsMissingRequestType() {
        assertThatThrownBy(() -> service.register("att_3", new DataSubjectRequest(null, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");

        verifyNoInteractions(candidateAttemptRepository, auditEventService);
    }

    @Test
    void rejectsUnknownAttempt() {
        when(candidateAttemptRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("ghost",
                new DataSubjectRequest(DataSubjectRequestType.PORTABILITY, null, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verifyNoInteractions(auditEventService);
    }
}
