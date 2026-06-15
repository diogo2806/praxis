package br.com.iforce.praxis.gupy.delivery.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "praxis.delivery-scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class ResultDeliveryProcessor {

    private final ResultDeliveryService resultDeliveryService;

    public ResultDeliveryProcessor(ResultDeliveryService resultDeliveryService) {
        this.resultDeliveryService = resultDeliveryService;
    }

    @Scheduled(fixedDelayString = "${praxis.delivery-scheduler-fixed-delay-ms:30000}")
    public void processReadyDeliveries() {
        resultDeliveryService.processReadyDeliveries();
    }
}
