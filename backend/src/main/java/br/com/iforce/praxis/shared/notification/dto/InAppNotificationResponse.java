package br.com.iforce.praxis.shared.notification.dto;

import br.com.iforce.praxis.shared.notification.model.InAppNotificationType;


import java.time.Instant;


public record InAppNotificationResponse(
        Long id,
        InAppNotificationType type,
        String title,
        String message,
        String candidateAttemptId,
        String candidateName,
        String candidateEmail,
        Long outboxEventId,
        Instant createdAt,
        Instant readAt
) {
}
