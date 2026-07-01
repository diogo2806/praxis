package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceListingEntity;
import br.com.iforce.praxis.simulation.dto.CloneSimulationVersionResponse;
import br.com.iforce.praxis.simulation.service.SimulationAdminService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceListingCloneService {

    private final SimulationAdminService simulationAdminService;

    public MarketplaceListingCloneService(SimulationAdminService simulationAdminService) {
        this.simulationAdminService = simulationAdminService;
    }

    @Transactional
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
