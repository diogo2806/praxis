package br.com.iforce.praxis.shared.notification.service;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;

import br.com.iforce.praxis.auth.persistence.repository.UserRepository;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.shared.notification.model.InAppNotificationType;

import br.com.iforce.praxis.shared.notification.persistence.entity.InAppNotificationEntity;

import br.com.iforce.praxis.shared.notification.persistence.repository.InAppNotificationRepository;

import br.com.iforce.praxis.shared.outbox.persistence.entity.OutboxEventEntity;

import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Propagation;

import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;

import java.util.List;

import java.util.Optional;


/**
 * Notifica administradores quando um resultado falha na entrega.
 *
 * Quando um evento entra em "Dead Letter Queue" (DLQ) porque falhou 5 vezes
 * de entregar o resultado para a Gupy, este serviço cria uma notificação
 * in-app para alertar os administradores da empresa.
 *
 * A notificação inclui detalhes do candidato afetado para que possam
 * investigar e corrigir o problema manualmente se necessário.
 */
@Slf4j
@Service
public class ResultDeliveryDlqAlertService {

    private static final String ADMIN_ROLE = "EMPRESA";

    private final UserRepository userRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final InAppNotificationRepository inAppNotificationRepository;
    private final ObjectMapper objectMapper;

    public ResultDeliveryDlqAlertService(
            UserRepository userRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            InAppNotificationRepository inAppNotificationRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.inAppNotificationRepository = inAppNotificationRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Cria notificações para alertar administradores de um evento não-entregável.
     *
     * Quando um evento não consegue ser entregue após várias tentativas,
     * todos os administradores da empresa recebem uma notificação alertando
     * que:
     * - Um resultado de candidato não foi enviado para a Gupy
     * - O sistema tentou várias vezes mas falhou
     * - Precisa de intervenção manual para corrigir
     *
     * Evita notificar o mesmo administrador mais de uma vez para o mesmo evento.
     *
     * Este método DEVE ser chamado dentro de uma transação de banco de dados existente.
     *
     * @param event O evento que falhou em todas as tentativas de entrega
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void alertEmpresaAdmins(OutboxEventEntity event) {
        String attemptId = resolveAttemptId(event);
        CandidateImpact impact = resolveCandidateImpact(event.getEmpresaId(), attemptId);
        List<UserEntity> admins = userRepository.findByEmpresaIdAndRole(event.getEmpresaId(), ADMIN_ROLE);

        if (admins.isEmpty()) {
            log.error(
                    "Evento {} entrou em DLQ, mas nenhum administrador foi encontrado para empresa={}",
                    event.getId(),
                    event.getEmpresaId()
            );
            return;
        }

        for (UserEntity admin : admins) {
            if (alreadyNotified(event, admin)) {
                continue;
            }
            createInAppNotification(event, impact, admin);
        }
    }

    private boolean alreadyNotified(OutboxEventEntity event, UserEntity admin) {
        return inAppNotificationRepository.existsByEmpresaIdAndOutboxEventIdAndRecipientUserIdAndType(
                event.getEmpresaId(),
                event.getId(),
                admin.getId(),
                InAppNotificationType.RESULT_DELIVERY_DLQ
        );
    }

    private void createInAppNotification(OutboxEventEntity event, CandidateImpact impact, UserEntity admin) {
        InAppNotificationEntity notification = new InAppNotificationEntity();
        notification.setEmpresaId(event.getEmpresaId());
        notification.setRecipientUserId(admin.getId());
        notification.setType(InAppNotificationType.RESULT_DELIVERY_DLQ);
        notification.setTitle("Resultado retido na integração com a Gupy");
        notification.setMessage(messageFor(impact));
        notification.setCandidateAttemptId(impact.attemptId());
        notification.setCandidateName(impact.candidateName());
        notification.setCandidateEmail(impact.candidateEmail());
        notification.setOutboxEventId(event.getId());
        notification.setCreatedAt(Instant.now());

        inAppNotificationRepository.save(notification);
    }

    private String messageFor(CandidateImpact impact) {
        return "O resultado de " + impact.candidateName()
                + " (" + impact.candidateEmail()
                + ") falhou nas tentativas automaticas de envio para a Gupy e precisa de suporte.";
    }

    private CandidateImpact resolveCandidateImpact(String empresaId, String attemptId) {
        Optional<CandidateAttemptEntity> attempt = candidateAttemptRepository.findByEmpresaIdAndId(empresaId, attemptId);
        if (attempt.isEmpty()) {
            return new CandidateImpact(attemptId, "Candidato não localizado", "email-nao-localizado@praxis.local", "resultado-nao-localizado");
        }

        CandidateAttemptEntity entity = attempt.get();
        return new CandidateImpact(
                entity.getId(),
                entity.getCandidateName(),
                entity.getCandidateEmail(),
                entity.getResultId()
        );
    }

    private String resolveAttemptId(OutboxEventEntity event) {
        if (event.getAggregateId() != null && !event.getAggregateId().isBlank()) {
            return event.getAggregateId();
        }

        try {
            JsonNode payload = objectMapper.readTree(event.getPayload());
            JsonNode attemptId = payload.get("attemptId");
            if (attemptId != null && !attemptId.isNull() && !attemptId.asText().isBlank()) {
                return attemptId.asText();
            }
        } catch (Exception exception) {
            log.warn("Não foi possível extrair attemptId do evento outbox {}", event.getId(), exception);
        }

        return "attempt-nao-localizado";
    }

    private record CandidateImpact(
            String attemptId,
            String candidateName,
            String candidateEmail,
            String resultId
    ) {
    }
}
