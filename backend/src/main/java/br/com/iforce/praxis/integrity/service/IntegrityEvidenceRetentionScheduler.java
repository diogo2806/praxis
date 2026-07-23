package br.com.iforce.praxis.integrity.service;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "praxis.integrity.evidence-retention-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class IntegrityEvidenceRetentionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntegrityEvidenceRetentionScheduler.class);

    private final EmpresaRepository empresaRepository;
    private final IntegrityEvidenceRetentionService retentionService;

    public IntegrityEvidenceRetentionScheduler(
            EmpresaRepository empresaRepository,
            IntegrityEvidenceRetentionService retentionService
    ) {
        this.empresaRepository = empresaRepository;
        this.retentionService = retentionService;
    }

    @Scheduled(cron = "${praxis.integrity.evidence-retention-cron:0 0 4 * * *}")
    public void discardExpiredEvidence() {
        empresaRepository.findAll().forEach(empresa -> {
            try {
                int discarded = retentionService.discardExpiredEvidenceForEmpresa(empresa.getId());
                if (discarded > 0) {
                    LOGGER.info(
                            "Descartadas {} sessoes tecnicas vencidas da empresa {}",
                            discarded,
                            empresa.getId()
                    );
                }
            } catch (RuntimeException exception) {
                LOGGER.warn(
                        "Falha ao descartar evidencias tecnicas da empresa {}: {}",
                        empresa.getId(),
                        exception.getMessage()
                );
            }
        });
    }
}
