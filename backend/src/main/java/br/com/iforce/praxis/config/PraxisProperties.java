package br.com.iforce.praxis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "praxis")
public record PraxisProperties(
        String publicBaseUrl,
        String integrationToken,
        int attemptLinkTtlHours,
        int attemptSessionTtlHours,
        int recommendInterviewThreshold,
        double competencyWeightTolerance,
        int gupyRateLimitRequestsPerMinute,
        int authoringRateLimitRequestsPerMinute
) {

    public PraxisProperties {
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
    }
}
