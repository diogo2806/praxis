package br.com.iforce.praxis.campaign.service;

import br.com.iforce.praxis.candidate.service.CompanyCandidateLinkService;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.notification.service.EmailDeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ParticipationCampaignOutboxWorkerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private CompanyCandidateLinkService candidateLinkService;

    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    @Mock
    private CampaignMessageTemplateService templateService;

    @Mock
    private EmailDeliveryService emailDeliveryService;

    private ParticipationCampaignOutboxWorker worker;

    @BeforeEach
    void setUp() {
        worker = new ParticipationCampaignOutboxWorker(
                jdbcTemplate,
                candidateLinkService,
                candidateAttemptRepository,
                templateService,
                emailDeliveryService
        );
    }

    @Test
    void shouldUseJdbcTimestampsWhenSearchingDueMessages() {
        doReturn(List.of()).when(jdbcTemplate).query(
                anyString(),
                ArgumentMatchers.<RowMapper<UUID>>any(),
                any(),
                any(),
                any()
        );

        worker.processDueMessages();

        ArgumentCaptor<Object> nextAttemptAtCaptor = ArgumentCaptor.forClass(Object.class);
        ArgumentCaptor<Object> scheduledAtCaptor = ArgumentCaptor.forClass(Object.class);
        verify(jdbcTemplate).query(
                anyString(),
                ArgumentMatchers.<RowMapper<UUID>>any(),
                nextAttemptAtCaptor.capture(),
                scheduledAtCaptor.capture(),
                eq(50)
        );

        assertThat(nextAttemptAtCaptor.getValue()).isInstanceOf(Timestamp.class);
        assertThat(scheduledAtCaptor.getValue()).isInstanceOf(Timestamp.class);
        assertThat(((Timestamp) nextAttemptAtCaptor.getValue()).toInstant())
                .isEqualTo(((Timestamp) scheduledAtCaptor.getValue()).toInstant());
    }
}
