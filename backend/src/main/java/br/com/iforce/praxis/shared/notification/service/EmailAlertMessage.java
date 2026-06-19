package br.com.iforce.praxis.shared.notification.service;

public record EmailAlertMessage(
        String tenantId,
        String to,
        String subject,
        String body
) {
}
