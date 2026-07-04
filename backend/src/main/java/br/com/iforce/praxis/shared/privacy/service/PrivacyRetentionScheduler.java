package br.com.iforce.praxis.shared.privacy.service;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import org.springframework.scheduling.annotation.Scheduled;

import org.springframework.stereotype.Component;


/**
 * Agenda a rotina automática de retenção de privacidade para todas as empresas.
 *
 * <p>Na visão do processo, este componente é o "relógio" da LGPD no Praxis. Em
 * vez de depender de alguém clicar em um botão, ele roda em um horário definido,
 * passa empresa por empresa e aciona a anonimização das tentativas que já
 * ultrapassaram o prazo de retenção.</p>
 *
 * <p>A rotina é tolerante a falhas por empresa: se a anonimização de uma empresa
 * falhar, o problema é registrado no log e as demais empresas continuam sendo
 * verificadas.</p>
 */
@Component
@ConditionalOnProperty(name = "praxis.privacy-retention-enabled", havingValue = "true", matchIfMissing = true)
public class PrivacyRetentionScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrivacyRetentionScheduler.class);

    private final EmpresaRepository empresaRepository;
    private final PrivacyRetentionService privacyRetentionService;

    /**
     * Monta o agendador com acesso à lista de empresas e ao serviço de retenção.
     *
     * <p>No processo operacional, o repositório informa quais empresas precisam
     * ser verificadas e o serviço de retenção executa a anonimização de cada uma.
     * O agendador apenas coordena a chamada periódica entre essas duas partes.</p>
     *
     * @param empresaRepository fonte da lista de empresas ativas no sistema
     * @param privacyRetentionService serviço que anonimiza tentativas vencidas de uma empresa
     */
    public PrivacyRetentionScheduler(
            EmpresaRepository empresaRepository,
            PrivacyRetentionService privacyRetentionService
    ) {
        this.empresaRepository = empresaRepository;
        this.privacyRetentionService = privacyRetentionService;
    }

    /**
     * Executa a varredura automática de retenção configurada para o ambiente.
     *
     * <p>Por padrão, o Praxis roda este processo diariamente às 03:30. A cada
     * execução, o sistema percorre todas as empresas cadastradas e pede que o
     * serviço de privacidade anonimiza as tentativas vencidas daquela empresa.</p>
     *
     * <p>Quando uma empresa tem registros anonimizados, a quantidade é registrada
     * no log operacional. Se alguma empresa falhar, a falha é registrada como
     * alerta e o processo segue para a próxima, evitando que um problema isolado
     * interrompa toda a rotina de privacidade.</p>
     */
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
