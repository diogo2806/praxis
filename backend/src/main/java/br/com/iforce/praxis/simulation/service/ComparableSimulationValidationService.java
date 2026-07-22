package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import org.springframework.stereotype.Service;

/**
 * Preserva a validação padrão para fluxos que precisam considerar
 * diferenças de pontuação entre caminhos como parte do modelo situacional,
 * sem transformá-las em bloqueio de publicação.
 */
@Service
public class ComparableSimulationValidationService extends SimulationValidationService {

    public ComparableSimulationValidationService(PraxisProperties praxisProperties) {
        super(praxisProperties);
    }
}
