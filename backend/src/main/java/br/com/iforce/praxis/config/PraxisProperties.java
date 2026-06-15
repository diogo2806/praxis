package br.com.iforce.praxis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "praxis")
public record PraxisProperties(String publicBaseUrl, String integrationToken) {
}
