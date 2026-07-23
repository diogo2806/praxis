package br.com.iforce.praxis.integrity.service;

import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.integrity.dto.IntegrityReviewDecisionRequest;
import br.com.iforce.praxis.integrity.model.IntegrityReviewDecision;
import br.com.iforce.praxis.integrity.model.IntegrityReviewStatus;
import br.com.iforce.praxis.integrity.persistence.entity.CandidateIntegrityReviewEntity;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityEventRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityReviewAuditRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegrityReviewRepository;
import br.com.iforce.praxis.integrity.persistence.repository.CandidateIntegritySessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrityReviewServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-23T18:00:00Z");

    @Mock
    private CandidateIntegrityReviewRepository reviewRepository;
    @Mock
    private CandidateIntegrityReviewAuditRepository reviewAuditRepository;
    @Mock
    private CandidateIntegrityEventRepository eventRepository;
    @Mock
    private CandidateIntegritySessionRepository sessionRepository;
    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private CurrentEmpresaService currentEmpresaService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private AuditEventService auditEventService;

    private CandidateIntegrityReviewEntity review;

    @BeforeEach
    void setUp() {
        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(eventRepository.findByEmpresaIdOrderByOccurredAtAscIdAsc(anyString(), any()))
                .thenReturn(new PageImpl<>(List.of()));
        review = pendingReview();
        when(reviewRepository.findByEmpresaIdAndCandidateAttemptId("empresa-1", "att-1"))
                .thenReturn(Optional.of(review));
    }

    @Test
    void requiresTextualJustification() {
        assertThatThrownBy(() -> service().decide(
                "att-1",
                new IntegrityReviewDecisionRequest(IntegrityReviewDecision.NO_IMPACT, "   ", false)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("justificativa");

        verify(reviewRepository, never()).save(any(CandidateIntegrityReviewEntity.class));
    }

    @Test
    void recordsResponsibleAndDoesNotChangeCandidateAttempt() {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att-1");
        attempt.setEmpresaId("empresa-1");
        attempt.setCandidateName("Pessoa Candidata");
        attempt.setCandidateEmail("candidate@example.com");
        attempt.setStatus(AttemptStatus.COMPLETED);
        when(candidateAttemptRepository.findByEmpresaIdAndId("empresa-1", "att-1"))
                .thenReturn(Optional.of(attempt));
        when(currentUserService.requiredUserId()).thenReturn("reviewer-1");
        when(sessionRepository.findByEmpresaIdAndCandidateAttemptIdOrderByStartedAtAsc("empresa-1", "att-1"))
                .thenReturn(List.of());
        when(eventRepository.findByEmpresaIdAndCandidateAttemptIdOrderByOccurredAtAscIdAsc("empresa-1", "att-1"))
                .thenReturn(List.of());
        when(reviewAuditRepository.findByEmpresaIdAndCandidateAttemptIdOrderByCreatedAtAscIdAsc("empresa-1", "att-1"))
                .thenReturn(List.of());

        service().decide(
                "att-1",
                new IntegrityReviewDecisionRequest(
                        IntegrityReviewDecision.REAPPLICATION_RECOMMENDED,
                        "A conexão foi interrompida durante a execução.",
                        true
                )
        );

        assertThat(review.getStatus()).isEqualTo(IntegrityReviewStatus.DECIDED);
        assertThat(review.getDecision()).isEqualTo(IntegrityReviewDecision.REAPPLICATION_RECOMMENDED);
        assertThat(review.getReviewedBy()).isEqualTo("reviewer-1");
        assertThat(review.getDecidedAt()).isEqualTo(NOW);
        verify(reviewRepository).save(review);
        verify(candidateAttemptRepository, never()).save(any(CandidateAttemptEntity.class));
        verify(reviewAuditRepository).save(any());
        verify(auditEventService).appendCandidateAttemptEvent(
                anyString(),
                anyString(),
                any(),
                anyString(),
                anyString()
        );
    }

    private IntegrityReviewService service() {
        return new IntegrityReviewService(
                reviewRepository,
                reviewAuditRepository,
                eventRepository,
                sessionRepository,
                candidateAttemptRepository,
                currentEmpresaService,
                currentUserService,
                auditEventService,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                180
        );
    }

    private CandidateIntegrityReviewEntity pendingReview() {
        CandidateIntegrityReviewEntity entity = new CandidateIntegrityReviewEntity();
        entity.setId("review-1");
        entity.setEmpresaId("empresa-1");
        entity.setCandidateAttemptId("att-1");
        entity.setStatus(IntegrityReviewStatus.PENDING);
        entity.setCreatedAt(NOW.minusSeconds(60));
        entity.setUpdatedAt(NOW.minusSeconds(60));
        entity.setRetentionUntil(NOW.plusSeconds(3600));
        return entity;
    }
}
