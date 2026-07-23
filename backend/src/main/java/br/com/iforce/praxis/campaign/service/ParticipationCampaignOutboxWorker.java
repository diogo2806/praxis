package br.com.iforce.praxis.campaign.service;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
import br.com.iforce.praxis.candidate.service.CompanyCandidateLinkService;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.notification.service.EmailDeliveryService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Component
public class ParticipationCampaignOutboxWorker {

    private static final int BATCH_SIZE = 50;
    private static final int MAX_ATTEMPTS = 3;

    private final JdbcTemplate jdbcTemplate;
    private final CompanyCandidateLinkService candidateLinkService;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final CampaignMessageTemplateService templateService;
    private final EmailDeliveryService emailDeliveryService;

    public ParticipationCampaignOutboxWorker(
            JdbcTemplate jdbcTemplate,
            CompanyCandidateLinkService candidateLinkService,
            CandidateAttemptRepository candidateAttemptRepository,
            CampaignMessageTemplateService templateService,
            EmailDeliveryService emailDeliveryService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.candidateLinkService = candidateLinkService;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.templateService = templateService;
        this.emailDeliveryService = emailDeliveryService;
    }

    @Scheduled(fixedDelayString = "${praxis.campaign.outbox-delay-ms:60000}")
    public void processDueMessages() {
        List<UUID> due = jdbcTemplate.query(
                """
                        SELECT outbox.id
                        FROM participation_campaign_outbox outbox
                        JOIN participation_campaigns campaign ON campaign.id = outbox.campaign_id
                        WHERE outbox.status IN ('PENDING', 'FAILED')
                          AND outbox.next_attempt_at <= ?
                          AND outbox.scheduled_at <= ?
                          AND campaign.status IN ('SCHEDULED', 'RUNNING')
                        ORDER BY outbox.next_attempt_at, outbox.id
                        LIMIT ?
                        """,
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class),
                Instant.now(),
                Instant.now(),
                BATCH_SIZE
        );
        for (UUID id : due) {
            processOne(id);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processOne(UUID outboxId) {
        int claimed = jdbcTemplate.update(
                """
                        UPDATE participation_campaign_outbox
                        SET status = 'PROCESSING', attempt_count = attempt_count + 1, processed_at = ?
                        WHERE id = ? AND status IN ('PENDING', 'FAILED')
                        """,
                Instant.now(),
                outboxId
        );
        if (claimed == 0) return;
        OutboxRow row = load(outboxId);
        EmpresaContextHolder.set(row.empresaId());
        try {
            if (!shouldSend(row)) {
                markSkipped(row);
                return;
            }
            LinkData link = ensureLink(row);
            CampaignMessageTemplateService.RenderedMessage message = templateService.renderMessage(
                    row.subjectTemplate(),
                    row.bodyTemplate(),
                    row.candidateName(),
                    row.recipientEmail(),
                    link.url(),
                    row.campaignName(),
                    row.simulationId(),
                    link.expiresAt()
            );
            boolean delivered = emailDeliveryService.sendPlainText(
                    row.recipientEmail(),
                    message.subject(),
                    message.body()
            );
            Instant now = Instant.now();
            jdbcTemplate.update(
                    """
                            UPDATE participation_campaign_outbox
                            SET status = 'SENT', subject_text = ?, body_text = ?, provider_delivered = ?,
                                sent_at = ?, processed_at = ?, last_error = NULL
                            WHERE id = ?
                            """,
                    message.subject(),
                    message.body(),
                    delivered,
                    now,
                    now,
                    row.id()
            );
            jdbcTemplate.update(
                    """
                            UPDATE participation_campaign_participants
                            SET communication_status = CASE WHEN ? THEN 'DELIVERED' ELSE communication_status END,
                                participation_status = CASE WHEN participation_status = 'PENDING' THEN 'NOT_STARTED' ELSE participation_status END,
                                updated_at = ?
                            WHERE id = ?
                            """,
                    delivered,
                    now,
                    row.participantId()
            );
            jdbcTemplate.update(
                    "UPDATE participation_campaigns SET status = CASE WHEN status = 'SCHEDULED' THEN 'RUNNING' ELSE status END WHERE id = ?",
                    row.campaignId()
            );
        } catch (RuntimeException exception) {
            markFailed(row, exception);
        } finally {
            EmpresaContextHolder.clear();
        }
    }

    @Scheduled(cron = "${praxis.campaign.retention-cron:0 30 3 * * *}")
    public void purgeExpiredPersonalData() {
        Instant now = Instant.now();
        List<UUID> campaigns = jdbcTemplate.query(
                """
                        SELECT id FROM participation_campaigns
                        WHERE retention_until <= ? AND status IN ('COMPLETED', 'CANCELLED')
                          AND EXISTS (
                              SELECT 1 FROM participation_campaign_participants participant
                              WHERE participant.campaign_id = participation_campaigns.id
                                AND (participant.candidate_name IS NOT NULL OR participant.candidate_email IS NOT NULL)
                          )
                        LIMIT 100
                        """,
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class),
                now
        );
        for (UUID campaignId : campaigns) {
            jdbcTemplate.update(
                    """
                            UPDATE participation_campaign_participants
                            SET candidate_name = NULL, candidate_email = NULL, candidate_url = NULL,
                                last_error = NULL, updated_at = ?
                            WHERE campaign_id = ?
                            """,
                    now,
                    campaignId
            );
            jdbcTemplate.update(
                    """
                            UPDATE participation_campaign_outbox
                            SET recipient_email = NULL, subject_text = NULL, body_text = NULL, last_error = NULL
                            WHERE campaign_id = ?
                            """,
                    campaignId
            );
        }
    }

    private LinkData ensureLink(OutboxRow row) {
        if (row.attemptId() != null && row.candidateUrl() != null && row.linkExpiresAt() != null) {
            return new LinkData(row.attemptId(), row.candidateUrl(), row.linkExpiresAt());
        }
        if (!"INITIAL".equals(row.messageType())) {
            throw new IllegalStateException("O convite inicial ainda não gerou o link da pessoa participante.");
        }
        CreateCandidateLinkResponse response = candidateLinkService.createNewApplication(
                new CreateCandidateLinkRequest(
                        row.candidateName(),
                        row.recipientEmail(),
                        row.simulationId(),
                        row.applicationCycleId(),
                        row.applicationContext(),
                        row.accommodationMultiplier()
                )
        );
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = issuedAt.plus(row.linkValidityDays(), ChronoUnit.DAYS);
        CandidateAttemptEntity attempt = candidateAttemptRepository.findOneByEmpresaIdAndId(
                        row.empresaId(),
                        response.attemptId()
                )
                .orElseThrow(() -> new IllegalStateException("A tentativa criada não foi encontrada."));
        attempt.setCandidateTokenIssuedAt(issuedAt);
        attempt.setCandidateTokenExpiresAt(expiresAt);
        candidateAttemptRepository.saveAndFlush(attempt);
        jdbcTemplate.update(
                """
                        UPDATE participation_campaign_participants
                        SET attempt_id = ?, candidate_url = ?, link_expires_at = ?,
                            participation_status = 'LINK_CREATED', updated_at = ?
                        WHERE id = ?
                        """,
                response.attemptId(),
                response.candidateUrl(),
                expiresAt,
                Instant.now(),
                row.participantId()
        );
        return new LinkData(response.attemptId(), response.candidateUrl(), expiresAt);
    }

    private boolean shouldSend(OutboxRow row) {
        if ("INITIAL".equals(row.messageType())) return true;
        if (row.attemptId() == null) return false;
        CandidateAttemptEntity attempt = candidateAttemptRepository.findByEmpresaIdAndId(
                row.empresaId(),
                row.attemptId()
        ).orElse(null);
        if (attempt == null || attempt.getStatus() == AttemptStatus.COMPLETED) return false;
        return switch (row.targetState()) {
            case "NOT_OPENED" -> row.openedAt() == null;
            case "NOT_STARTED" -> attempt.getStatus() == AttemptStatus.NOT_STARTED;
            case "IN_PROGRESS" -> attempt.getStatus() == AttemptStatus.IN_PROGRESS;
            default -> false;
        };
    }

    private void markSkipped(OutboxRow row) {
        Instant now = Instant.now();
        jdbcTemplate.update(
                "UPDATE participation_campaign_outbox SET status = 'SKIPPED', processed_at = ?, last_error = NULL WHERE id = ?",
                now,
                row.id()
        );
    }

    private void markFailed(OutboxRow row, RuntimeException exception) {
        Instant now = Instant.now();
        String message = sanitizeError(exception);
        if (row.attemptCount() >= MAX_ATTEMPTS) {
            jdbcTemplate.update(
                    """
                            UPDATE participation_campaign_outbox
                            SET status = 'DEAD_LETTER', last_error = ?, processed_at = ?
                            WHERE id = ?
                            """,
                    message,
                    now,
                    row.id()
            );
            jdbcTemplate.update(
                    """
                            UPDATE participation_campaign_participants
                            SET communication_status = 'FAILED',
                                participation_status = CASE WHEN ? = 'INITIAL' AND participation_status = 'PENDING' THEN 'FAILED' ELSE participation_status END,
                                last_error = ?, updated_at = ?
                            WHERE id = ?
                            """,
                    row.messageType(),
                    message,
                    now,
                    row.participantId()
            );
            return;
        }
        long retryMinutes = Math.min(60, 1L << Math.max(0, row.attemptCount()));
        jdbcTemplate.update(
                """
                        UPDATE participation_campaign_outbox
                        SET status = 'FAILED', last_error = ?, next_attempt_at = ?, processed_at = ?
                        WHERE id = ?
                        """,
                message,
                now.plus(retryMinutes, ChronoUnit.MINUTES),
                now,
                row.id()
        );
    }

    private OutboxRow load(UUID id) {
        List<OutboxRow> rows = jdbcTemplate.query(
                """
                        SELECT outbox.id, outbox.empresa_id, outbox.campaign_id, outbox.participant_id,
                               outbox.message_type, outbox.reminder_index, outbox.target_state,
                               outbox.recipient_email, outbox.attempt_count,
                               campaign.name AS campaign_name, campaign.simulation_id,
                               campaign.application_cycle_id, campaign.application_context,
                               campaign.link_validity_days, campaign.subject_template AS campaign_subject,
                               campaign.body_template AS campaign_body,
                               participant.candidate_name, participant.accommodation_multiplier,
                               participant.attempt_id, participant.candidate_url, participant.link_expires_at,
                               participant.opened_at,
                               reminder.subject_template AS reminder_subject,
                               reminder.body_template AS reminder_body
                        FROM participation_campaign_outbox outbox
                        JOIN participation_campaigns campaign ON campaign.id = outbox.campaign_id
                        JOIN participation_campaign_participants participant ON participant.id = outbox.participant_id
                        LEFT JOIN participation_campaign_reminders reminder
                          ON reminder.campaign_id = outbox.campaign_id
                         AND reminder.reminder_index = outbox.reminder_index
                        WHERE outbox.id = ?
                        """,
                (resultSet, rowNum) -> new OutboxRow(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("empresa_id"),
                        resultSet.getObject("campaign_id", UUID.class),
                        resultSet.getObject("participant_id", UUID.class),
                        resultSet.getString("message_type"),
                        resultSet.getInt("reminder_index"),
                        resultSet.getString("target_state"),
                        resultSet.getString("recipient_email"),
                        resultSet.getInt("attempt_count"),
                        resultSet.getString("campaign_name"),
                        resultSet.getString("simulation_id"),
                        resultSet.getString("application_cycle_id"),
                        resultSet.getString("application_context"),
                        resultSet.getInt("link_validity_days"),
                        "REMINDER".equals(resultSet.getString("message_type"))
                                ? resultSet.getString("reminder_subject")
                                : resultSet.getString("campaign_subject"),
                        "REMINDER".equals(resultSet.getString("message_type"))
                                ? resultSet.getString("reminder_body")
                                : resultSet.getString("campaign_body"),
                        resultSet.getString("candidate_name"),
                        resultSet.getBigDecimal("accommodation_multiplier") == null
                                ? BigDecimal.ONE
                                : resultSet.getBigDecimal("accommodation_multiplier"),
                        resultSet.getString("attempt_id"),
                        resultSet.getString("candidate_url"),
                        timestamp(resultSet, "link_expires_at"),
                        timestamp(resultSet, "opened_at")
                ),
                id
        );
        if (rows.isEmpty()) throw new IllegalStateException("Mensagem da outbox não encontrada.");
        return rows.getFirst();
    }

    private Instant timestamp(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        java.sql.Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String sanitizeError(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return exception.getClass().getSimpleName();
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    private record OutboxRow(
            UUID id,
            String empresaId,
            UUID campaignId,
            UUID participantId,
            String messageType,
            int reminderIndex,
            String targetState,
            String recipientEmail,
            int attemptCount,
            String campaignName,
            String simulationId,
            String applicationCycleId,
            String applicationContext,
            int linkValidityDays,
            String subjectTemplate,
            String bodyTemplate,
            String candidateName,
            BigDecimal accommodationMultiplier,
            String attemptId,
            String candidateUrl,
            Instant linkExpiresAt,
            Instant openedAt
    ) {
    }

    private record LinkData(String attemptId, String url, Instant expiresAt) {
    }
}
