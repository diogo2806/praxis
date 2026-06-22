package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "praxis.privacy-retention-enabled", havingValue = "true", matchIfMissing = true)
public class PrivacyRetentionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivacyRetentionScheduler.class);

    private final TenantRepository tenantRepository;
    private final PrivacyRetentionService privacyRetentionService;

    public PrivacyRetentionScheduler(
            TenantRepository tenantRepository,
            PrivacyRetentionService privacyRetentionService
    ) {
        this.tenantRepository = tenantRepository;
        this.privacyRetentionService = privacyRetentionService;
    }

    @Scheduled(cron = "${praxis.privacy-retention-cron:0 30 3 * * *}")
    public void anonymizeExpiredAttempts() {
        tenantRepository.findAll().forEach(tenant -> {
            try {
                int anonymizedCount = privacyRetentionService.anonymizeExpiredAttemptsForTenant(tenant.getId());
                if (anonymizedCount > 0) {
                    LOGGER.info("Anonimizadas {} tentativas vencidas do tenant {}", anonymizedCount, tenant.getId());
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Falha ao executar retencao de privacidade para tenant {}: {}", tenant.getId(), exception.getMessage());
            }
        });
    }
}
