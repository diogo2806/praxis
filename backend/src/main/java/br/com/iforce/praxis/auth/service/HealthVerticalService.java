package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import org.springframework.stereotype.Service;


/**
 * Resolve se um empresa opera na vertical de saúde (uso educativo). A vertical muda o regime
 * jurídico: exige aceite do termo de uso em saúde pelo recrutador antes de publicar e coleta de
 * consentimento do paciente no fluxo do candidato (LGPD, dado sensível — arts. 11 e 14).
 *
 * <p>Hoje a flag é definida no empresa (via configuração/migração). Empresas sem a flag mantêm o
 * comportamento atual.</p>
 */
@Service
public class HealthVerticalService {

    private final EmpresaRepository empresaRepository;

    public HealthVerticalService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    /**
     * Indica se a empresa opera na vertical de saúde (uso educativo).
     *
     * <p>Quando verdadeiro, o processo passa a exigir regras adicionais de
     * privacidade: aceite do termo de uso em saúde pelo recrutador antes de
     * publicar e consentimento do participante para dados sensíveis. Empresas
     * sem essa marcação seguem o comportamento padrão.</p>
     *
     * @param empresaId identificador da empresa
     * @return {@code true} se a empresa está na vertical de saúde
     */
    public boolean isHealthVertical(String empresaId) {
        if (empresaId == null || empresaId.isBlank()) {
            return false;
        }
        return empresaRepository.findById(empresaId)
                .map(empresa -> empresa.isHealthVertical())
                .orElse(false);
    }
}
