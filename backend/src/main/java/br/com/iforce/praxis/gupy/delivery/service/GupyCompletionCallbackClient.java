package br.com.iforce.praxis.gupy.delivery.service;

public interface GupyCompletionCallbackClient {

    void notifyCompletion(String callbackUrl);
}
