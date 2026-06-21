package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.model.HumanDecision;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.candidate.dto.RegisterDispositionRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CandidateDispositionServiceTest {

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    @Mock
    private AuditEventService auditEventService;

    @Mock
    private CurrentTenantService currentTenantService;

    @Mock
    private CurrentUserService currentUserService;

    private CandidateDispositionService service;

    @BeforeEach
    void setUp() {
        service = new CandidateDispositionService(
                candidateAttemptRepository,
                auditEventService,
                currentTenantService,
                currentUserService,
                new ObjectMapper()
        );
    }

    @Test
    void registersHumanDecisionAsAppendOnlyAuditEvent() {
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(currentUserService.requiredUserId()).thenReturn("42");
        when(candidateAttemptRepository.findByTenantIdAndId("tenant-1", "att_123"))
                .thenReturn(Optional.of(new CandidateAttemptEntity()));

        service.register("att_123", new RegisterDispositionRequest(HumanDecision.REJECTED, "Não cumpriu o critério crítico."));

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("tenant-1"),
                eq("att_123"),
                eq(AuditEventType.HUMAN_DECISION),
                messageCaptor.capture(),
                metadataCaptor.capture()
        );

        assertThat(messageCaptor.getValue()).contains("REJECTED").contains("42");
        assertThat(metadataCaptor.getValue())
                .contains("\"decision\":\"REJECTED\"")
                .contains("\"decidedByUserId\":\"42\"")
                .contains("\"attemptId\":\"att_123\"")
                .contains("Não cumpriu o critério crítico.")
                .contains("decidedAt");
    }

    @Test
    void rejectsDecisionWhenAttemptDoesNotBelongToTenant() {
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(currentUserService.requiredUserId()).thenReturn("42");
        when(candidateAttemptRepository.findByTenantIdAndId("tenant-1", "ghost"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("ghost", new RegisterDispositionRequest(HumanDecision.ADVANCED, null)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");

        verify(auditEventService, never()).appendCandidateAttemptEvent(
                eq("tenant-1"), eq("ghost"), eq(AuditEventType.HUMAN_DECISION), eq(null), eq(null));
    }

    @Test
    void normalizesBlankReasonToNull() {
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(currentUserService.requiredUserId()).thenReturn("7");
        when(candidateAttemptRepository.findByTenantIdAndId("tenant-1", "att_9"))
                .thenReturn(Optional.of(new CandidateAttemptEntity()));

        service.register("att_9", new RegisterDispositionRequest(HumanDecision.ON_HOLD, "   "));

        ArgumentCaptor<String> metadataCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditEventService).appendCandidateAttemptEvent(
                eq("tenant-1"), eq("att_9"), eq(AuditEventType.HUMAN_DECISION), org.mockito.ArgumentMatchers.anyString(), metadataCaptor.capture());

        assertThat(metadataCaptor.getValue()).contains("\"reason\":null");
    }

    @Test
    void doesNothingElseWhenAttemptMissing() {
        when(currentTenantService.requiredTenantId()).thenReturn("tenant-1");
        when(currentUserService.requiredUserId()).thenReturn("1");
        when(candidateAttemptRepository.findByTenantIdAndId("tenant-1", "x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("x", new RegisterDispositionRequest(HumanDecision.HIRED, null)))
                .isInstanceOf(ResponseStatusException.class);

        verifyNoInteractions(auditEventService);
    }
}
