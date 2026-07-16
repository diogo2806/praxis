package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.gupy.dto.TestResultResponse;

public interface ResultWebhookClient {

    void postResult(String webhookUrl, TestResultResponse testResultResponse);

    void postPayload(String webhookUrl, Object payload);
}
