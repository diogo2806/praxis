package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.config.PraxisProperties;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Mantém o serviço padrão como implementação primária da validação.
 * Diferenças de pontuação entre caminhos são parte do modelo situacional
 * e não constituem bloqueio de publicação.
 */
@Primary
@Service
public class ComparableSimulationValidationService extends SimulationValidationService {

    public ComparableSimulationValidationService(PraxisProperties praxisProperties) {
        super(praxisProperties);
    }
}
