package br.com.iforce.praxis.config;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.web.client.RestClientCustomizer;

import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;

import org.springframework.http.client.SimpleClientHttpRequestFactory;


import java.time.Duration;


/**
 * Timeouts de conexão/leitura para TODAS as chamadas HTTP de saída feitas via {@code RestClient}
 * (webhook de resultado da Gupy, webhook CUSTOM_API e API do Mercado Pago).
 *
 * <p>Sem timeout explícito, um destino lento ou pendurado travaria indefinidamente a thread do
 * poller do outbox — que processa o lote sequencialmente — paralisando todas as demais entregas e
 * alimentando o cenário de reivindicação de eventos "órfãos" por outra instância.</p>
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClientCustomizer outboundHttpTimeoutCustomizer(
            @Value("${praxis.http.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${praxis.http.read-timeout-ms:15000}") int readTimeoutMs
    ) {
        return builder -> {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
            factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
            builder.requestFactory(factory);
        };
    }
}
