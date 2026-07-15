package br.com.iforce.praxis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

@ConfigurationProperties(prefix = "praxis")
public record PraxisProperties(
        String publicBaseUrl,
        String candidatePageBaseUrl,
        int attemptLinkTtlHours,
        int attemptSessionTtlHours,
        int candidateResultTtlHours,
        int recommendInterviewThreshold,
        int answerGracePeriodSeconds,
        double competencyWeightTolerance
) {

    public PraxisProperties(
            String publicBaseUrl,
            int attemptLinkTtlHours,
            int attemptSessionTtlHours,
            int recommendInterviewThreshold,
            int answerGracePeriodSeconds,
            double competencyWeightTolerance
    ) {
        this(
                publicBaseUrl,
                publicBaseUrl,
                attemptLinkTtlHours,
                attemptSessionTtlHours,
                720,
                recommendInterviewThreshold,
                answerGracePeriodSeconds,
                competencyWeightTolerance
        );
    }

    @ConstructorBinding
    public PraxisProperties {
        if (candidatePageBaseUrl == null || candidatePageBaseUrl.isBlank()) {
            candidatePageBaseUrl = publicBaseUrl;
        }
        if (candidateResultTtlHours == 0) {
            candidateResultTtlHours = 720;
        }
        if (candidateResultTtlHours < 0) {
            throw new IllegalArgumentException("praxis.candidate-result-ttl-hours deve ser maior que zero.");
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
    }
}
