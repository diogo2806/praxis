package br.com.iforce.praxis.shared.notification.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "praxis.alert-email.smtp-enabled", havingValue = "true")
public class SmtpEmailAlertSender implements EmailAlertSender {

    private final JavaMailSender mailSender;

    public SmtpEmailAlertSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(EmailAlertMessage message) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(message.to());
        mailMessage.setSubject(message.subject());
        mailMessage.setText(message.body());
        mailSender.send(mailMessage);
    }
}
