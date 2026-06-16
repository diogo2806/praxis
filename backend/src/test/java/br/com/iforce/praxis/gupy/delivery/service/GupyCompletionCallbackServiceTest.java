package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GupyCompletionCallbackServiceTest {

    @Test
    void notifyCompletionCallsCallbackUrlWhenEnabled() {
        GupyCompletionCallbackClient callbackClient = mock(GupyCompletionCallbackClient.class);
        GupyCompletionCallbackService service = new GupyCompletionCallbackService(callbackClient, true);

        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att_123");
        attempt.setCallbackUrl("https://cliente.gupy.io/callback");

        service.notifyCompletionIfNeeded(attempt);

        verify(callbackClient, timeout(TimeUnit.SECONDS.toMillis(1)))
                .notifyCompletion("https://cliente.gupy.io/callback");
    }

    @Test
    void notifyCompletionDoesNothingWhenDisabled() {
        GupyCompletionCallbackClient callbackClient = mock(GupyCompletionCallbackClient.class);
        GupyCompletionCallbackService service = new GupyCompletionCallbackService(callbackClient, false);

        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId("att_123");
        attempt.setCallbackUrl("https://cliente.gupy.io/callback");

        service.notifyCompletionIfNeeded(attempt);

        verifyNoInteractions(callbackClient);
    }
}
