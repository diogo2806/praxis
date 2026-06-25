package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import org.springframework.stereotype.Service;

/**
 * Resolve se um tenant opera na vertical de saúde (uso educativo). A vertical muda o regime
 * jurídico: exige aceite do termo de uso em saúde pelo recrutador antes de publicar e coleta de
 * consentimento do paciente no fluxo do candidato (LGPD, dado sensível — arts. 11 e 14).
 *
 * <p>Hoje a flag é definida no tenant (via configuração/migração). Tenants sem a flag mantêm o
 * comportamento atual.</p>
 */
@Service
public class HealthVerticalService {

    private final TenantRepository tenantRepository;

    public HealthVerticalService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Indica se a empresa opera na vertical de saúde (uso educativo).
     *
     * <p>Quando verdadeiro, o processo passa a exigir regras adicionais de
     * privacidade: aceite do termo de uso em saúde pelo recrutador antes de
     * publicar e consentimento do participante para dados sensíveis. Empresas
     * sem essa marcação seguem o comportamento padrão.</p>
     *
     * @param tenantId identificador da empresa
     * @return {@code true} se a empresa está na vertical de saúde
     */
    public boolean isHealthVertical(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        return tenantRepository.findById(tenantId)
                .map(tenant -> tenant.isHealthVertical())
                .orElse(false);
    }
}
