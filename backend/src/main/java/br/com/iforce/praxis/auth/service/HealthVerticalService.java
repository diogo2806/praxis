package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import org.springframework.stereotype.Service;

/** Resolve a habilitação efetiva da vertical educativa de saúde. */
@Service
public class HealthVerticalService {

    private final EmpresaRepository empresaRepository;

    public HealthVerticalService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    /**
     * A flag técnica isolada não habilita o tratamento de dados sensíveis. A
     * empresa também precisa possuir aprovação formal, datada e vinculada ao
     * usuário responsável.
     */
    public boolean isHealthVertical(String empresaId) {
        if (empresaId == null || empresaId.isBlank()) {
            return false;
        }
        return empresaRepository.findById(empresaId)
                .map(empresa -> empresa.isHealthVertical()
                        && empresa.getHealthComplianceApprovedAt() != null
                        && empresa.getHealthComplianceApprovedBy() != null
                        && !empresa.getHealthComplianceApprovedBy().isBlank())
                .orElse(false);
    }
}
