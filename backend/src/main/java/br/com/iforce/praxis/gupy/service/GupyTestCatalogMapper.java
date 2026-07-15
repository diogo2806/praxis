package br.com.iforce.praxis.gupy.service;

import br.com.iforce.praxis.gupy.dto.GupyTestResponse;
import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import org.springframework.stereotype.Component;

/**
 * Adapta uma simulação publicada ao contrato de catálogo da Gupy.
 *
 * <p>Categoria e nível são opcionais no contrato externo. Como esses metadados
 * ainda não possuem fonte configurável no domínio publicado, eles devem ser
 * omitidos em vez de receber valores genéricos aplicados a todas as avaliações.</p>
 */
@Component
public class GupyTestCatalogMapper {

    public GupyTestResponse toResponse(PublishedSimulation simulation) {
        return new GupyTestResponse(
                simulation.id(),
                simulation.name(),
                null,
                simulation.description(),
                null
        );
    }
}
