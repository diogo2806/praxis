package br.com.iforce.praxis.billing.service;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;


import java.time.Instant;


/**
 * Dispara os lembretes recorrentes da régua de cobrança antes da suspensão dura do acesso.
 *
 * <p>Enquanto o toque imediato de falha nasce do webhook do Mercado Pago, este agendamento cuida do
 * "não deixar esfriar": roda na frequência configurada ({@code praxis.billing.dunning-reminder-cron},
 * por padrão às 09:00) e relembra os clientes que seguem pendentes de pagamento ou inadimplentes.
 * Segue o mesmo padrão dos demais agendamentos e pode ser desligado por configuração
 * ({@code praxis.billing.dunning-enabled=false}).</p>
 */
@Component
@ConditionalOnProperty(name = "praxis.billing.dunning-enabled", havingValue = "true", matchIfMissing = true)
public class DunningReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(DunningReminderScheduler.class);

    private final BillingDunningService dunningService;

    public DunningReminderScheduler(BillingDunningService dunningService) {
        this.dunningService = dunningService;
    }

    @Scheduled(cron = "${praxis.billing.dunning-reminder-cron:0 0 9 * * *}")
    public void sendDunningReminders() {
        try {
            dunningService.remindClientsBeforeSuspension(Instant.now());
        } catch (RuntimeException exception) {
            log.warn("Falha ao enviar os lembretes da régua de cobrança: {}", exception.getMessage(), exception);
        }
    }
}
