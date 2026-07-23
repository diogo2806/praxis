package br.com.iforce.praxis.campaign.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.billing.service.CreditService;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CampaignParticipantInput;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CampaignParticipantResponse;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CampaignResponse;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CampaignTotals;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CommunicationEvent;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CreateCampaignRequest;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CsvPreviewRequest;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CsvPreviewResponse;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.CsvRowDiagnostic;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.MessagePreviewRequest;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.MessagePreviewResponse;
import br.com.iforce.praxis.campaign.dto.ParticipationCampaignDtos.ReminderRuleInput;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.gupy.service.IdempotencyKeyHasher;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ParticipationCampaignService {

    private final JdbcTemplate jdbcTemplate;
    private final CampaignCsvParser csvParser;
    private final CampaignMessageTemplateService templateService;
    private final SimulationCatalogService simulationCatalogService;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final EmpresaRepository empresaRepository;
    private final CreditService creditService;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;

    public ParticipationCampaignService(
            JdbcTemplate jdbcTemplate,
            CampaignCsvParser csvParser,
            CampaignMessageTemplateService templateService,
            SimulationCatalogService simulationCatalogService,
            CandidateAttemptRepository candidateAttemptRepository,
            EmpresaRepository empresaRepository,
            CreditService creditService,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.csvParser = csvParser;
        this.templateService = templateService;
        this.simulationCatalogService = simulationCatalogService;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.empresaRepository = empresaRepository;
        this.creditService = creditService;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public CsvPreviewResponse previewCsv(CsvPreviewRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        requirePublishedSimulation(empresaId, request.simulationId());
        CampaignCsvParser.ParsedCsv parsed = csvParser.parse(request.csvContent(), request.consentRequired());
        if (!parsed.headerErrors().isEmpty()) {
            return new CsvPreviewResponse(
                    parsed.headers(),
                    0,
                    0,
                    parsed.headerErrors().size(),
                    availableCapacity(empresaId),
                    false,
                    parsed.headerErrors().stream()
                            .map(error -> new CsvRowDiagnostic(1, "", "", false, List.of(error), List.of()))
                            .toList(),
                    List.of()
            );
        }

        List<CsvRowDiagnostic> diagnostics = new ArrayList<>();
        List<CampaignParticipantInput> valid = new ArrayList<>();
        for (CampaignCsvParser.ParsedRow row : parsed.rows()) {
            List<String> errors = new ArrayList<>(row.diagnostic().errors());
            List<String> warnings = new ArrayList<>(row.diagnostic().warnings());
            CampaignParticipantInput participant = row.participant();
            if (errors.isEmpty() && hasExistingApplication(
                    empresaId,
                    request.simulationId(),
                    request.applicationCycleId(),
                    participant.email()
            )) {
                if (request.allowExistingActive()) {
                    warnings.add("Já existe aplicação para a pessoa neste ciclo; o pedido será reaproveitado de forma idempotente.");
                } else {
                    errors.add("Já existe aplicação para a pessoa, avaliação e ciclo informados.");
                }
            }
            boolean rowValid = errors.isEmpty();
            diagnostics.add(new CsvRowDiagnostic(
                    participant.rowNumber(),
                    participant.name(),
                    participant.email(),
                    rowValid,
                    List.copyOf(errors),
                    List.copyOf(warnings)
            ));
            if (rowValid) valid.add(participant);
        }
        int capacity = availableCapacity(empresaId);
        boolean exceeded = capacity >= 0 && valid.size() > capacity;
        if (exceeded) {
            diagnostics.add(new CsvRowDiagnostic(
                    0,
                    "",
                    "",
                    false,
                    List.of("A campanha possui " + valid.size() + " participantes válidos, mas o plano permite iniciar " + capacity + " nova(s) avaliação(ões) neste momento."),
                    List.of()
            ));
        }
        int invalidRows = Math.toIntExact(diagnostics.stream().filter(row -> !row.valid()).filter(row -> row.rowNumber() > 0).count());
        return new CsvPreviewResponse(
                parsed.headers(),
                parsed.rows().size(),
                valid.size(),
                invalidRows,
                capacity,
                exceeded,
                List.copyOf(diagnostics),
                exceeded ? List.of() : List.copyOf(valid)
        );
    }

    @Transactional(readOnly = true)
    public MessagePreviewResponse previewMessage(MessagePreviewRequest request) {
        return templateService.preview(
                request.subjectTemplate(),
                request.bodyTemplate(),
                request.sampleName(),
                request.campaignName()
        );
    }

    @Transactional
    public CampaignResponse create(CreateCampaignRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actor = currentUserService.requiredUserId();
        requirePublishedSimulation(empresaId, request.simulationId());
        templateService.validate(request.subjectTemplate(), request.bodyTemplate());
        validateReminders(request.reminders());
        validateParticipants(request);
        validateCapacity(empresaId, request.participants().size());

        List<UUID> existing = jdbcTemplate.query(
                "SELECT id FROM participation_campaigns WHERE empresa_id = ? AND idempotency_key = ?",
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class),
                empresaId,
                request.idempotencyKey().trim()
        );
        if (!existing.isEmpty()) {
            return get(existing.getFirst());
        }

        UUID campaignId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant sendAt = request.initialSendAt().isBefore(now) ? now : request.initialSendAt();
        String status = sendAt.isAfter(now) ? "SCHEDULED" : "RUNNING";
        Instant retentionUntil = now.plus(request.retentionDays(), ChronoUnit.DAYS);
        jdbcTemplate.update(
                """
                        INSERT INTO participation_campaigns (
                            id, empresa_id, name, simulation_id, application_cycle_id,
                            application_context, idempotency_key, status, initial_send_at,
                            link_validity_days, consent_required, allow_existing_active,
                            subject_template, body_template, retention_until, created_by, created_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                campaignId,
                empresaId,
                request.name().trim(),
                request.simulationId().trim(),
                request.applicationCycleId().trim(),
                trimToNull(request.applicationContext()),
                request.idempotencyKey().trim(),
                status,
                sendAt,
                request.linkValidityDays(),
                request.consentRequired(),
                request.allowExistingActive(),
                request.subjectTemplate().trim(),
                request.bodyTemplate().trim(),
                retentionUntil,
                actor,
                now
        );

        List<ReminderRuleInput> reminders = request.reminders() == null ? List.of() : request.reminders();
        for (ReminderRuleInput reminder : reminders) {
            templateService.validate(reminder.subjectTemplate(), reminder.bodyTemplate());
            jdbcTemplate.update(
                    """
                            INSERT INTO participation_campaign_reminders (
                                campaign_id, reminder_index, send_after_hours, target_state,
                                subject_template, body_template
                            ) VALUES (?, ?, ?, ?, ?, ?)
                            """,
                    campaignId,
                    reminder.reminderIndex(),
                    reminder.sendAfterHours(),
                    reminder.targetState().name(),
                    reminder.subjectTemplate().trim(),
                    reminder.bodyTemplate().trim()
            );
        }

        for (CampaignParticipantInput participant : request.participants()) {
            UUID participantId = UUID.randomUUID();
            String normalizedEmail = normalizeEmail(participant.email());
            String emailHash = IdempotencyKeyHasher.sha256Hex(normalizedEmail);
            jdbcTemplate.update(
                    """
                            INSERT INTO participation_campaign_participants (
                                id, campaign_id, row_number, candidate_name, candidate_email,
                                candidate_email_hash, consent_confirmed, accommodation_multiplier,
                                participation_status, communication_status, created_at, updated_at
                            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 'QUEUED', ?, ?)
                            """,
                    participantId,
                    campaignId,
                    participant.rowNumber(),
                    participant.name().trim(),
                    normalizedEmail,
                    emailHash,
                    participant.consentConfirmed(),
                    participant.accommodationMultiplier(),
                    now,
                    now
            );
            insertOutbox(
                    empresaId,
                    campaignId,
                    participantId,
                    "INITIAL",
                    0,
                    null,
                    sendAt,
                    normalizedEmail
            );
            for (ReminderRuleInput reminder : reminders) {
                insertOutbox(
                        empresaId,
                        campaignId,
                        participantId,
                        "REMINDER",
                        reminder.reminderIndex(),
                        reminder.targetState().name(),
                        sendAt.plus(reminder.sendAfterHours(), ChronoUnit.HOURS),
                        normalizedEmail
                );
            }
        }
        return get(campaignId);
    }

    @Transactional(readOnly = true)
    public List<CampaignResponse> list() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return jdbcTemplate.query(
                "SELECT id FROM participation_campaigns WHERE empresa_id = ? ORDER BY created_at DESC",
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class),
                empresaId
        ).stream().map(this::get).toList();
    }

    @Transactional(readOnly = true)
    public CampaignResponse get(UUID campaignId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        CampaignRow campaign = findCampaign(empresaId, campaignId);
        syncParticipantStatuses(campaignId, empresaId);
        List<CampaignParticipantResponse> participants = loadParticipants(campaignId);
        List<Map<String, Object>> reminders = loadReminders(campaignId);
        return toResponse(campaign, participants, reminders);
    }

    @Transactional
    public CampaignResponse pause(UUID campaignId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        CampaignRow campaign = findCampaign(empresaId, campaignId);
        if (!Set.of("SCHEDULED", "RUNNING").contains(campaign.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente campanhas agendadas ou em execução podem ser pausadas.");
        }
        jdbcTemplate.update("UPDATE participation_campaigns SET status = 'PAUSED', paused_at = ? WHERE id = ?", Instant.now(), campaignId);
        return get(campaignId);
    }

    @Transactional
    public CampaignResponse resume(UUID campaignId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        CampaignRow campaign = findCampaign(empresaId, campaignId);
        if (!"PAUSED".equals(campaign.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A campanha não está pausada.");
        }
        jdbcTemplate.update("UPDATE participation_campaigns SET status = 'RUNNING', paused_at = NULL WHERE id = ?", campaignId);
        return get(campaignId);
    }

    @Transactional
    public CampaignResponse cancel(UUID campaignId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        CampaignRow campaign = findCampaign(empresaId, campaignId);
        if (Set.of("COMPLETED", "CANCELLED").contains(campaign.status())) {
            return get(campaignId);
        }
        Instant now = Instant.now();
        jdbcTemplate.update("UPDATE participation_campaigns SET status = 'CANCELLED', cancelled_at = ? WHERE id = ?", now, campaignId);
        jdbcTemplate.update(
                """
                        UPDATE participation_campaign_outbox
                        SET status = 'CANCELLED', processed_at = ?
                        WHERE campaign_id = ? AND status IN ('PENDING', 'FAILED', 'PROCESSING')
                        """,
                now,
                campaignId
        );
        jdbcTemplate.update(
                """
                        UPDATE participation_campaign_participants
                        SET participation_status = CASE WHEN participation_status = 'COMPLETED' THEN 'COMPLETED' ELSE 'CANCELLED' END,
                            cancelled_at = CASE WHEN participation_status = 'COMPLETED' THEN cancelled_at ELSE ? END,
                            updated_at = ?
                        WHERE campaign_id = ?
                        """,
                now,
                now,
                campaignId
        );
        return get(campaignId);
    }

    @Transactional
    public CampaignResponse registerCommunicationEvent(
            UUID campaignId,
            UUID participantId,
            CommunicationEvent event
    ) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        findCampaign(empresaId, campaignId);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM participation_campaign_participants WHERE campaign_id = ? AND id = ?",
                Integer.class,
                campaignId,
                participantId
        );
        if (count == null || count == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Participante não encontrado na campanha.");
        }
        Instant now = Instant.now();
        switch (event) {
            case DELIVERED -> jdbcTemplate.update(
                    "UPDATE participation_campaign_participants SET communication_status = 'DELIVERED', updated_at = ? WHERE id = ?",
                    now,
                    participantId
            );
            case BOUNCED -> jdbcTemplate.update(
                    "UPDATE participation_campaign_participants SET communication_status = 'BOUNCED', last_error = 'Provedor informou bounce.', updated_at = ? WHERE id = ?",
                    now,
                    participantId
            );
            case OPENED -> jdbcTemplate.update(
                    "UPDATE participation_campaign_participants SET communication_status = 'OPENED', opened_at = COALESCE(opened_at, ?), updated_at = ? WHERE id = ?",
                    now,
                    now,
                    participantId
            );
        }
        return get(campaignId);
    }

    @Transactional(readOnly = true)
    public byte[] exportOperationalCsv(UUID campaignId) {
        CampaignResponse campaign = get(campaignId);
        StringBuilder csv = new StringBuilder();
        csv.append("linha;nome;email;tentativa;participacao;comunicacao;expiracao;erro\n");
        for (CampaignParticipantResponse participant : campaign.participants()) {
            appendCsv(
                    csv,
                    participant.rowNumber(),
                    participant.candidateName(),
                    participant.maskedEmail(),
                    participant.attemptId(),
                    participant.participationStatus(),
                    participant.communicationStatus(),
                    participant.linkExpiresAt(),
                    participant.lastError()
            );
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void validateParticipants(CreateCampaignRequest request) {
        Set<String> emails = new HashSet<>();
        for (CampaignParticipantInput participant : request.participants()) {
            String email = normalizeEmail(participant.email());
            if (!emails.add(email)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail duplicado na confirmação: " + email);
            }
            if (request.consentRequired() && !participant.consentConfirmed()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consentimento obrigatório ausente para " + email + ".");
            }
            if (!request.allowExistingActive() && hasExistingApplication(
                    currentEmpresaService.requiredEmpresaId(),
                    request.simulationId(),
                    request.applicationCycleId(),
                    email
            )) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Já existe aplicação ativa para " + email + " neste ciclo. Faça nova pré-validação ou permita reaproveitamento explícito."
                );
            }
        }
    }

    private void validateReminders(List<ReminderRuleInput> reminders) {
        if (reminders == null) return;
        Set<Integer> indexes = new HashSet<>();
        for (ReminderRuleInput reminder : reminders) {
            if (!indexes.add(reminder.reminderIndex())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Índice de lembrete duplicado: " + reminder.reminderIndex());
            }
            templateService.validate(reminder.subjectTemplate(), reminder.bodyTemplate());
        }
    }

    private void validateCapacity(String empresaId, int requested) {
        int capacity = availableCapacity(empresaId);
        if (capacity >= 0 && requested > capacity) {
            throw new ResponseStatusException(
                    HttpStatus.PAYMENT_REQUIRED,
                    "A campanha requer " + requested + " vagas, mas o plano permite iniciar " + capacity + " nova(s) avaliação(ões)."
            );
        }
    }

    private int availableCapacity(String empresaId) {
        EmpresaEntity empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null || !isMetered(empresa.getCommercialPlanType())) {
            return -1;
        }
        int balance = creditService.getBalance(empresaId);
        long active = candidateAttemptRepository.countByEmpresaIdAndStatusIn(
                empresaId,
                List.of(AttemptStatus.NOT_STARTED, AttemptStatus.IN_PROGRESS)
        );
        return Math.max(0, balance - Math.toIntExact(active));
    }

    private boolean isMetered(CommercialPlanType planType) {
        return planType == CommercialPlanType.AVULSO || planType == CommercialPlanType.PROFISSIONAL;
    }

    private boolean hasExistingApplication(
            String empresaId,
            String simulationId,
            String cycleId,
            String email
    ) {
        String key = IdempotencyKeyHasher.sha256Hex(
                empresaId + "|company-application|" + normalizeEmail(email) + "|"
                        + simulationId.trim() + "|" + cycleId.trim()
        );
        return candidateAttemptRepository.findByEmpresaIdAndIdempotencyKey(empresaId, key).isPresent();
    }

    private void requirePublishedSimulation(String empresaId, String simulationId) {
        simulationCatalogService.findPublishedById(empresaId, simulationId.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avaliação publicada não encontrada."));
    }

    private void insertOutbox(
            String empresaId,
            UUID campaignId,
            UUID participantId,
            String type,
            int reminderIndex,
            String targetState,
            Instant scheduledAt,
            String recipient
    ) {
        String idempotency = IdempotencyKeyHasher.sha256Hex(
                empresaId + "|campaign-message|" + campaignId + "|" + participantId + "|" + type + "|" + reminderIndex
        );
        jdbcTemplate.update(
                """
                        INSERT INTO participation_campaign_outbox (
                            id, empresa_id, campaign_id, participant_id, message_type,
                            reminder_index, target_state, scheduled_at, recipient_email,
                            idempotency_key, status, attempt_count, next_attempt_at
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', 0, ?)
                        """,
                UUID.randomUUID(),
                empresaId,
                campaignId,
                participantId,
                type,
                reminderIndex,
                targetState,
                scheduledAt,
                recipient,
                idempotency,
                scheduledAt
        );
    }

    private CampaignRow findCampaign(String empresaId, UUID id) {
        List<CampaignRow> rows = jdbcTemplate.query(
                """
                        SELECT id, empresa_id, name, simulation_id, application_cycle_id,
                               application_context, status, initial_send_at, link_validity_days,
                               consent_required, allow_existing_active, subject_template, body_template,
                               retention_until, created_by, created_at
                        FROM participation_campaigns
                        WHERE id = ? AND empresa_id = ?
                        """,
                (resultSet, rowNum) -> new CampaignRow(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("empresa_id"),
                        resultSet.getString("name"),
                        resultSet.getString("simulation_id"),
                        resultSet.getString("application_cycle_id"),
                        resultSet.getString("application_context"),
                        resultSet.getString("status"),
                        resultSet.getTimestamp("initial_send_at").toInstant(),
                        resultSet.getInt("link_validity_days"),
                        resultSet.getBoolean("consent_required"),
                        resultSet.getBoolean("allow_existing_active"),
                        resultSet.getString("subject_template"),
                        resultSet.getString("body_template"),
                        resultSet.getTimestamp("retention_until").toInstant(),
                        resultSet.getString("created_by"),
                        resultSet.getTimestamp("created_at").toInstant()
                ),
                id,
                empresaId
        );
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campanha não encontrada.");
        }
        return rows.getFirst();
    }

    private List<CampaignParticipantResponse> loadParticipants(UUID campaignId) {
        return jdbcTemplate.query(
                """
                        SELECT id, row_number, candidate_name, candidate_email, consent_confirmed,
                               attempt_id, candidate_url, link_expires_at, participation_status,
                               communication_status, last_error, opened_at, started_at, completed_at,
                               expired_at, cancelled_at
                        FROM participation_campaign_participants
                        WHERE campaign_id = ?
                        ORDER BY row_number
                        """,
                (resultSet, rowNum) -> new CampaignParticipantResponse(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getInt("row_number"),
                        resultSet.getString("candidate_name"),
                        maskEmail(resultSet.getString("candidate_email")),
                        resultSet.getBoolean("consent_confirmed"),
                        resultSet.getString("attempt_id"),
                        resultSet.getString("candidate_url"),
                        timestamp(resultSet, "link_expires_at"),
                        resultSet.getString("participation_status"),
                        resultSet.getString("communication_status"),
                        resultSet.getString("last_error"),
                        timestamp(resultSet, "opened_at"),
                        timestamp(resultSet, "started_at"),
                        timestamp(resultSet, "completed_at"),
                        timestamp(resultSet, "expired_at"),
                        timestamp(resultSet, "cancelled_at")
                ),
                campaignId
        );
    }

    private List<Map<String, Object>> loadReminders(UUID campaignId) {
        return jdbcTemplate.query(
                """
                        SELECT reminder_index, send_after_hours, target_state, subject_template
                        FROM participation_campaign_reminders
                        WHERE campaign_id = ?
                        ORDER BY reminder_index
                        """,
                (resultSet, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("reminderIndex", resultSet.getInt("reminder_index"));
                    row.put("sendAfterHours", resultSet.getInt("send_after_hours"));
                    row.put("targetState", resultSet.getString("target_state"));
                    row.put("subjectTemplate", resultSet.getString("subject_template"));
                    return Map.copyOf(row);
                },
                campaignId
        );
    }

    private CampaignResponse toResponse(
            CampaignRow campaign,
            List<CampaignParticipantResponse> participants,
            List<Map<String, Object>> reminders
    ) {
        CampaignTotals totals = totals(participants);
        return new CampaignResponse(
                campaign.id(),
                campaign.name(),
                campaign.simulationId(),
                campaign.applicationCycleId(),
                campaign.applicationContext(),
                campaign.status(),
                campaign.initialSendAt(),
                campaign.linkValidityDays(),
                campaign.consentRequired(),
                campaign.allowExistingActive(),
                campaign.retentionUntil(),
                campaign.createdBy(),
                campaign.createdAt(),
                totals,
                participants,
                reminders
        );
    }

    private CampaignTotals totals(List<CampaignParticipantResponse> participants) {
        Map<String, Integer> participation = new HashMap<>();
        Map<String, Integer> communication = new HashMap<>();
        participants.forEach(item -> {
            participation.merge(item.participationStatus(), 1, Integer::sum);
            communication.merge(item.communicationStatus(), 1, Integer::sum);
        });
        return new CampaignTotals(
                participants.size(),
                participation.getOrDefault("PENDING", 0),
                communication.getOrDefault("DELIVERED", 0),
                communication.getOrDefault("FAILED", 0),
                communication.getOrDefault("BOUNCED", 0),
                communication.getOrDefault("OPENED", 0),
                participation.getOrDefault("NOT_STARTED", 0) + participation.getOrDefault("LINK_CREATED", 0),
                participation.getOrDefault("IN_PROGRESS", 0),
                participation.getOrDefault("COMPLETED", 0),
                participation.getOrDefault("EXPIRED", 0),
                participation.getOrDefault("CANCELLED", 0)
        );
    }

    private void syncParticipantStatuses(UUID campaignId, String empresaId) {
        List<AttemptLink> links = jdbcTemplate.query(
                "SELECT id, attempt_id, link_expires_at FROM participation_campaign_participants WHERE campaign_id = ? AND attempt_id IS NOT NULL",
                (resultSet, rowNum) -> new AttemptLink(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("attempt_id"),
                        timestamp(resultSet, "link_expires_at")
                ),
                campaignId
        );
        Instant now = Instant.now();
        for (AttemptLink link : links) {
            CandidateAttemptEntity attempt = candidateAttemptRepository.findByEmpresaIdAndId(empresaId, link.attemptId()).orElse(null);
            if (attempt == null) continue;
            String status = mapAttemptStatus(attempt.getStatus(), link.expiresAt(), now);
            Instant startedAt = attempt.getStartedAt();
            Instant completedAt = attempt.getFinishedAt();
            jdbcTemplate.update(
                    """
                            UPDATE participation_campaign_participants
                            SET participation_status = ?,
                                started_at = COALESCE(started_at, ?),
                                completed_at = COALESCE(completed_at, ?),
                                expired_at = CASE WHEN ? = 'EXPIRED' THEN COALESCE(expired_at, ?) ELSE expired_at END,
                                updated_at = ?
                            WHERE id = ?
                            """,
                    status,
                    startedAt,
                    completedAt,
                    status,
                    now,
                    now,
                    link.id()
            );
        }
        Integer remaining = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM participation_campaign_participants
                        WHERE campaign_id = ? AND participation_status NOT IN ('COMPLETED', 'EXPIRED', 'CANCELLED', 'FAILED')
                        """,
                Integer.class,
                campaignId
        );
        if (remaining != null && remaining == 0) {
            jdbcTemplate.update(
                    "UPDATE participation_campaigns SET status = CASE WHEN status = 'CANCELLED' THEN status ELSE 'COMPLETED' END, completed_at = COALESCE(completed_at, ?) WHERE id = ?",
                    now,
                    campaignId
            );
        }
    }

    private String mapAttemptStatus(AttemptStatus status, Instant expiresAt, Instant now) {
        if (status == AttemptStatus.COMPLETED) return "COMPLETED";
        if (status == AttemptStatus.IN_PROGRESS) return "IN_PROGRESS";
        if (expiresAt != null && !expiresAt.isAfter(now)) return "EXPIRED";
        return "NOT_STARTED";
    }

    private String normalizeEmail(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String maskEmail(String value) {
        if (value == null || value.isBlank()) return "";
        int at = value.indexOf('@');
        if (at <= 1) return "***" + value.substring(Math.max(0, at));
        return value.substring(0, 1) + "***" + value.substring(at);
    }

    private Instant timestamp(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        java.sql.Timestamp value = resultSet.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void appendCsv(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) csv.append(';');
            String value = values[index] == null ? "" : values[index].toString();
            csv.append('"').append(value.replace("\"", "\"\"")).append('"');
        }
        csv.append('\n');
    }

    private record CampaignRow(
            UUID id,
            String empresaId,
            String name,
            String simulationId,
            String applicationCycleId,
            String applicationContext,
            String status,
            Instant initialSendAt,
            int linkValidityDays,
            boolean consentRequired,
            boolean allowExistingActive,
            String subjectTemplate,
            String bodyTemplate,
            Instant retentionUntil,
            String createdBy,
            Instant createdAt
    ) {
    }

    private record AttemptLink(UUID id, String attemptId, Instant expiresAt) {
    }
}
