package br.com.iforce.praxis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "praxis")
public record PraxisProperties(
        String publicBaseUrl,
        String integrationToken,
        int attemptLinkTtlHours,
        int attemptSessionTtlHours,
        int recommendInterviewThreshold,
        double competencyWeightTolerance
) {

    public PraxisProperties {
        if (recommendInterviewThreshold == 0) {
            recommendInterviewThreshold = 70;
        }
        if (competencyWeightTolerance == 0) {
            competencyWeightTolerance = 0.001;
        }
    }
}
