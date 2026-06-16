package br.com.iforce.praxis.privacy.service;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class PrivacyRetentionService {

    private static final int BATCH_SIZE = 100;

    private final CandidateAttemptRepository candidateAttemptRepository;
    private final int retentionDays;
    private final boolean enabled;

    public PrivacyRetentionService(
            CandidateAttemptRepository candidateAttemptRepository,
            @Value("${praxis.privacy-retention-days:180}") int retentionDays,
            @Value("${praxis.privacy-retention-enabled:true}") boolean enabled
    ) {
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.retentionDays = retentionDays;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${praxis.privacy-retention-cron:0 30 3 * * *}")
    @Transactional
    public void anonymizeExpiredAttempts() {
        if (!enabled) {
            return;
        }

        Instant finishedBefore = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        List<CandidateAttemptEntity> attempts = candidateAttemptRepository
                .findByAnonymizedAtIsNullAndStatusInAndFinishedAtBefore(
                        List.of(
                                AttemptStatus.COMPLETED,
                                AttemptStatus.ABANDONED,
                                AttemptStatus.EXPIRED,
                                AttemptStatus.FAILED
                        ),
                        finishedBefore,
                        PageRequest.of(0, BATCH_SIZE)
                );

        Instant anonymizedAt = Instant.now();
        for (CandidateAttemptEntity attempt : attempts) {
            attempt.setCandidateName("Anonimizado");
            attempt.setCandidateEmail("anon+" + attempt.getId() + "@praxis.local");
            attempt.setCallbackUrl(null);
            attempt.setResultWebhookUrl(null);
            attempt.setAnonymizedAt(anonymizedAt);
        }
    }
}
