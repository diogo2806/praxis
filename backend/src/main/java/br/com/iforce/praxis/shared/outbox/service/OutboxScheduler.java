package br.com.iforce.praxis.shared.outbox.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(name = "praxis.outbox-scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class OutboxScheduler {

    private final OutboxProcessor outboxProcessor;

    public OutboxScheduler(OutboxProcessor outboxProcessor) {
        this.outboxProcessor = outboxProcessor;
    }

    @Scheduled(fixedDelayString = "${praxis.outbox-scheduler-fixed-delay-ms:5000}")
    public void processReadyEvents() {
        outboxProcessor.processReadyEvents();
    }
}
