package br.com.iforce.praxis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "praxis")
public record PraxisProperties(
        String publicBaseUrl,
        String integrationToken,
        int attemptLinkTtlHours,
        int attemptSessionTtlHours,
        int recommendInterviewThreshold,
        double competencyWeightTolerance,
        int gupyRateLimitRequestsPerMinute,
        int authoringRateLimitRequestsPerMinute,
        int authLoginRateLimitRequestsPerMinute,
        List<String> webhookAllowedHosts
) {

    public PraxisProperties {
        if (webhookAllowedHosts == null) {
            webhookAllowedHosts = List.of();
        }
        if (recommendInterviewThreshold == 0) {
            recommendInterviewThreshold = 70;
        }
        if (competencyWeightTolerance == 0) {
            competencyWeightTolerance = 0.001;
        }
        if (gupyRateLimitRequestsPerMinute == 0) {
            gupyRateLimitRequestsPerMinute = 100;
        }
        if (authoringRateLimitRequestsPerMinute == 0) {
            authoringRateLimitRequestsPerMinute = 30;
        }
        if (authLoginRateLimitRequestsPerMinute == 0) {
            authLoginRateLimitRequestsPerMinute = 10;
        }
    }
}
