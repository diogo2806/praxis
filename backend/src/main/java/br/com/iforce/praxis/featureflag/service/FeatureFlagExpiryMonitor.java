package br.com.iforce.praxis.featureflag.service;

import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.GovernanceSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FeatureFlagExpiryMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureFlagExpiryMonitor.class);

    private final FeatureFlagService featureFlagService;

    public FeatureFlagExpiryMonitor(FeatureFlagService featureFlagService) {
        this.featureFlagService = featureFlagService;
    }

    @Scheduled(fixedDelayString = "${praxis.feature-flags.expiry-check-ms:3600000}")
    public void reportExpiredFlags() {
        GovernanceSummary governance = featureFlagService.governance(null, null);
        if (!governance.expiredFlags().isEmpty()) {
            LOGGER.warn(
                    "Existem {} feature flags expiradas aguardando remoção: {}",
                    governance.expiredFlags().size(),
                    governance.expiredFlags().stream().map(flag -> flag.key()).toList()
            );
        }
    }
}
