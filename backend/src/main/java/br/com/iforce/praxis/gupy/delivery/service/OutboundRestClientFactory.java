package br.com.iforce.praxis.gupy.delivery.service;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

@Component
public class OutboundRestClientFactory {

    public RestClient create(ValidatedOutboundTarget target, int connectTimeoutMs, int readTimeoutMs) {
        String validatedHost = target.uri().getHost().toLowerCase(Locale.ROOT);
        InetAddress[] validatedAddresses = target.addresses();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(host -> resolvePinned(host, validatedHost, validatedAddresses))
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMilliseconds(Math.max(1, readTimeoutMs)))
                        .build())
                .setTlsSocketStrategy(ClientTlsStrategyBuilder.create().buildClassic())
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .disableRedirectHandling()
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory =
                new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(Math.max(1, connectTimeoutMs));
        requestFactory.setReadTimeout(Math.max(1, readTimeoutMs));

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private InetAddress[] resolvePinned(
            String requestedHost,
            String validatedHost,
            InetAddress[] validatedAddresses
    ) throws UnknownHostException {
        if (!requestedHost.equalsIgnoreCase(validatedHost)) {
            throw new UnknownHostException("Host não validado: " + requestedHost);
        }
        return validatedAddresses.clone();
    }
}
