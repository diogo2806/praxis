package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;


@Component
@ConditionalOnProperty(name = "praxis.privacy-retention-enabled", havingValue = "true", matchIfMissing = true)
public class PrivacyRetentionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivacyRetentionScheduler.class);

    private final EmpresaRepository empresaRepository;
    private final PrivacyRetentionService privacyRetentionService;

    public PrivacyRetentionScheduler(
            EmpresaRepository empresaRepository,
            PrivacyRetentionService privacyRetentionService
    ) {
        this.empresaRepository = empresaRepository;
        this.privacyRetentionService = privacyRetentionService;
    }

    @Scheduled(cron = "${praxis.privacy-retention-cron:0 30 3 * * *}")
    public void anonymizeExpiredAttempts() {
        empresaRepository.findAll().forEach(empresa -> {
            try {
                int anonymizedCount = privacyRetentionService.anonymizeExpiredAttemptsForEmpresa(empresa.getId());
                if (anonymizedCount > 0) {
                    LOGGER.info("Anonimizadas {} tentativas vencidas do empresa {}", anonymizedCount, empresa.getId());
                }
            } catch (RuntimeException exception) {
                LOGGER.warn("Falha ao executar retencao de privacidade para empresa {}: {}", empresa.getId(), exception.getMessage());
            }
        });
    }
}
