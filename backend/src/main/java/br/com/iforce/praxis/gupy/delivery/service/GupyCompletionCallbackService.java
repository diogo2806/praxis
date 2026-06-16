package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class GupyCompletionCallbackService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GupyCompletionCallbackService.class);

    private final GupyCompletionCallbackClient callbackClient;
    private final boolean enabled;

    public GupyCompletionCallbackService(
            GupyCompletionCallbackClient callbackClient,
            @Value("${praxis.callback-delivery-enabled:true}") boolean enabled
    ) {
        this.callbackClient = callbackClient;
        this.enabled = enabled;
    }

    public void notifyCompletionIfNeeded(CandidateAttemptEntity candidateAttemptEntity) {
        if (!enabled || candidateAttemptEntity.getCallbackUrl() == null || candidateAttemptEntity.getCallbackUrl().isBlank()) {
            return;
        }

        String callbackUrl = candidateAttemptEntity.getCallbackUrl();
        String attemptId = candidateAttemptEntity.getId();
        CompletableFuture.runAsync(() -> notifyCompletion(callbackUrl, attemptId));
    }

    private void notifyCompletion(String callbackUrl, String attemptId) {
        try {
            callbackClient.notifyCompletion(callbackUrl);
        } catch (RuntimeException exception) {
            LOGGER.warn("Falha ao notificar callbackUrl da Gupy para tentativa {}.", attemptId, exception);
        }
    }
}
