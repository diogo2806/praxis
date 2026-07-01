package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceListingEntity;
import br.com.iforce.praxis.simulation.dto.CloneSimulationVersionResponse;
import br.com.iforce.praxis.simulation.service.SimulationAdminService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * Copia para a empresa compradora a vers&atilde;o de simula&ccedil;&atilde;o associada a um item vendido no marketplace.
 *
 * <p>No processo de neg&oacute;cio, este servi&ccedil;o &eacute; o passo que entrega o conte&uacute;do adquirido ao
 * cliente ap&oacute;s a confirma&ccedil;&atilde;o do pagamento.</p>
 */
public class MarketplaceListingCloneService {

    private final SimulationAdminService simulationAdminService;

    public MarketplaceListingCloneService(SimulationAdminService simulationAdminService) {
        this.simulationAdminService = simulationAdminService;
    }

    @Transactional
    /**
     * Gera uma c&oacute;pia utiliz&aacute;vel do teste comprado dentro do tenant do cliente.
     *
     * <p>Isso permite que a empresa compradora passe a operar sua pr&oacute;pria vers&atilde;o do material,
     * sem editar diretamente o original publicado pelo profissional no marketplace.</p>
     */
    public CloneSimulationVersionResponse cloneListingToTenant(
            MarketplaceListingEntity listing,
            String destinationEmpresaId
    ) {
        return simulationAdminService.cloneVersionToTenant(
                listing.getSourceVersionId(),
                destinationEmpresaId
        );
    }
}
