package br.com.iforce.praxis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

import java.util.List;

@ConfigurationProperties(prefix = "praxis")
public record PraxisProperties(
        String publicBaseUrl,
        String candidatePageBaseUrl,
        String integrationToken,
        int attemptLinkTtlHours,
        int attemptSessionTtlHours,
        int recommendInterviewThreshold,
        int answerGracePeriodSeconds,
        double competencyWeightTolerance,
        int gupyRateLimitRequestsPerMinute,
        int authoringRateLimitRequestsPerMinute,
        int authLoginRateLimitRequestsPerMinute,
        List<String> webhookAllowedHosts
) {

    public PraxisProperties(
            String publicBaseUrl,
            String integrationToken,
            int attemptLinkTtlHours,
            int attemptSessionTtlHours,
            int recommendInterviewThreshold,
            int answerGracePeriodSeconds,
            double competencyWeightTolerance,
            int gupyRateLimitRequestsPerMinute,
            int authoringRateLimitRequestsPerMinute,
            int authLoginRateLimitRequestsPerMinute,
            List<String> webhookAllowedHosts
    ) {
        this(
                publicBaseUrl,
                publicBaseUrl,
                integrationToken,
                attemptLinkTtlHours,
                attemptSessionTtlHours,
                recommendInterviewThreshold,
                answerGracePeriodSeconds,
                competencyWeightTolerance,
                gupyRateLimitRequestsPerMinute,
                authoringRateLimitRequestsPerMinute,
                authLoginRateLimitRequestsPerMinute,
                webhookAllowedHosts
        );
    }

    @ConstructorBinding
    public PraxisProperties {
        if (candidatePageBaseUrl == null || candidatePageBaseUrl.isBlank()) {
            candidatePageBaseUrl = publicBaseUrl;
        }
        if (webhookAllowedHosts == null) {
            webhookAllowedHosts = List.of();
        }
        if (recommendInterviewThreshold == 0) {
            recommendInterviewThreshold = 70;
        }
        if (answerGracePeriodSeconds == 0) {
            answerGracePeriodSeconds = 15;
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
