package br.com.iforce.praxis.shared.notification.service;

public interface EmailAlertSender {

    void send(EmailAlertMessage message);
}
