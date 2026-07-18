package br.com.iforce.praxis.partner.service;

import br.com.iforce.praxis.gupy.model.PublishedSimulation;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.partner.persistence.entity.PartnerCatalogAccessEntity;
import br.com.iforce.praxis.partner.persistence.entity.PartnerClientEntity;
import br.com.iforce.praxis.partner.persistence.repository.PartnerCatalogAccessRepository;
import br.com.iforce.praxis.partner.persistence.repository.PartnerClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PartnerIntegrationCatalogService {

    private final PartnerClientRepository partnerClientRepository;
    private final PartnerCatalogAccessRepository partnerCatalogAccessRepository;
    private final SimulationCatalogService simulationCatalogService;

    public PartnerIntegrationCatalogService(
            PartnerClientRepository partnerClientRepository,
            PartnerCatalogAccessRepository partnerCatalogAccessRepository,
            SimulationCatalogService simulationCatalogService
    ) {
        this.partnerClientRepository = partnerClientRepository;
        this.partnerCatalogAccessRepository = partnerCatalogAccessRepository;
        this.simulationCatalogService = simulationCatalogService;
    }

    @Transactional(readOnly = true)
    public List<PublishedSimulation> findPublished(
            String empresaId,
            String clientId,
            String search,
            int offset,
            int limit
    ) {
        requireActiveClient(empresaId, clientId);
        String normalizedSearch = normalize(search);
        Set<String> assignedIds = assignedIds(empresaId, clientId);
        return simulationCatalogService.findPublished(empresaId).stream()
                .filter(simulation -> assignedIds.contains(simulation.id()))
                .filter(simulation -> normalizedSearch.isBlank()
                        || normalize(simulation.id()).contains(normalizedSearch)
                        || normalize(simulation.name()).contains(normalizedSearch)
                        || normalize(simulation.description()).contains(normalizedSearch))
                .skip(Math.max(offset, 0))
                .limit(Math.max(limit, 0))
                .toList();
    }

    @Transactional(readOnly = true)
    public int countPublished(String empresaId, String clientId, String search) {
        requireActiveClient(empresaId, clientId);
        String normalizedSearch = normalize(search);
        Set<String> assignedIds = assignedIds(empresaId, clientId);
        return (int) simulationCatalogService.findPublished(empresaId).stream()
                .filter(simulation -> assignedIds.contains(simulation.id()))
                .filter(simulation -> normalizedSearch.isBlank()
                        || normalize(simulation.id()).contains(normalizedSearch)
                        || normalize(simulation.name()).contains(normalizedSearch)
                        || normalize(simulation.description()).contains(normalizedSearch))
                .count();
    }

    @Transactional(readOnly = true)
    public void assertAccess(String empresaId, String clientId, String simulationId) {
        requireActiveClient(empresaId, clientId);
        if (!partnerCatalogAccessRepository
                .existsByEmpresaIdAndPartnerClientIdAndSimulationIdAndActiveTrue(
                        empresaId,
                        clientId,
                        simulationId
                )) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Teste não liberado para este cliente.");
        }
    }

    private Set<String> assignedIds(String empresaId, String clientId) {
        return partnerCatalogAccessRepository
                .findByEmpresaIdAndPartnerClientIdAndActiveTrueOrderByCreatedAtAsc(empresaId, clientId)
                .stream()
                .map(PartnerCatalogAccessEntity::getSimulationId)
                .collect(Collectors.toSet());
    }

    private PartnerClientEntity requireActiveClient(String empresaId, String clientId) {
        PartnerClientEntity client = partnerClientRepository.findByIdAndEmpresaId(clientId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cliente de integração inválido."));
        if (!client.isActive()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cliente de integração desativado.");
        }
        return client;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
