package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.marketplace.dto.CreateListingRequest;
import br.com.iforce.praxis.marketplace.dto.CreateListingResponse;
import br.com.iforce.praxis.marketplace.dto.ListingDetailResponse;
import br.com.iforce.praxis.marketplace.dto.ListingSearchFilter;
import br.com.iforce.praxis.marketplace.dto.ListingSummaryResponse;
import br.com.iforce.praxis.marketplace.dto.MarketplacePageResponse;
import br.com.iforce.praxis.marketplace.dto.UpdateListingRequest;
import br.com.iforce.praxis.marketplace.model.ListingCategory;
import br.com.iforce.praxis.marketplace.model.ListingStatus;
import br.com.iforce.praxis.marketplace.model.ProfessionalVerificationStatus;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceListingEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceListingRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationNodeEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationOptionEntity;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
/**
 * Gerencia o ciclo de vida dos testes publicados por profissionais no marketplace.
 *
 * <p>Na visão de negócio, este serviço cuida desde a criação do rascunho até a
 * disponibilização do item para compra, incluindo edição, envio para revisão e consulta
 * do catálogo público.</p>
 */
public class MarketplaceListingService {

    private static final String PLATFORM_EMPRESA_ID = "PLATFORM";

    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceProfessionalRepository professionalRepository;
    private final SimulationVersionRepository simulationVersionRepository;

    public MarketplaceListingService(
            MarketplaceListingRepository listingRepository,
            MarketplaceProfessionalRepository professionalRepository,
            SimulationVersionRepository simulationVersionRepository
    ) {
        this.listingRepository = listingRepository;
        this.professionalRepository = professionalRepository;
        this.simulationVersionRepository = simulationVersionRepository;
    }

    @Transactional
    /**
     * Cria um novo rascunho de item comercializável a partir de uma versão já publicada de simulação.
     *
     * <p>Esse é o momento em que o profissional define como seu material aparecerá para venda
     * no marketplace, sem ainda deixá-lo visível para clientes.</p>
     */
    public CreateListingResponse create(String userId, CreateListingRequest request) {
        MarketplaceProfessionalEntity professional = loadProfessional(userId);
        SimulationVersionEntity version = simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                        PLATFORM_EMPRESA_ID,
                        request.sourceSimulationId(),
                        request.sourceVersionNumber()
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Versao de simulacao nao encontrada para publicacao."
                ));

        if (version.getStatus() != SimulationVersionStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Apenas versoes publicadas podem virar listing.");
        }

        MarketplaceListingEntity listing = new MarketplaceListingEntity();
        listing.setProfessionalId(professional.getId());
        listing.setSourceSimulationId(version.getSimulation().getId());
        listing.setSourceVersionId(version.getId());
        listing.setTitle(request.title().trim());
        listing.setDescription(request.description().trim());
        listing.setCategory(request.category());
        listing.setPriceCents(request.priceCents());
        listing.setPreviewNodeIds(cleanPreviewNodes(request.previewNodeIds()));
        listing.setStatus(ListingStatus.DRAFT);
        listing = listingRepository.save(listing);

        return new CreateListingResponse(listing.getId(), listing.getStatus());
    }

    @Transactional
    /**
     * Envia um rascunho para moderação da plataforma.
     *
     * <p>No processo, isso marca que o profissional terminou a preparação do item e deseja
     * que a plataforma avalie se ele pode ser vendido.</p>
     */
    public CreateListingResponse submit(String userId, Long listingId) {
        MarketplaceProfessionalEntity professional = loadProfessional(userId);
        if (professional.getVerificationStatus() != ProfessionalVerificationStatus.VERIFIED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Profissional ainda nao verificado.");
        }

        MarketplaceListingEntity listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing nao encontrado."));
        if (!listing.getProfessionalId().equals(professional.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Listing pertence a outro profissional.");
        }
        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing nao esta em estado editavel.");
        }

        listing.setStatus(ListingStatus.PENDING_REVIEW);
        listing.setRejectionReason(null);
        return new CreateListingResponse(listing.getId(), listing.getStatus());
    }

    @Transactional
    /**
     * Atualiza os dados comerciais e de apresentação de um item ainda editável.
     *
     * <p>Esse fluxo existe para permitir ajustes antes da aprovação final ou correções após
     * uma rejeição da moderação.</p>
     */
    public CreateListingResponse update(String userId, Long listingId, UpdateListingRequest request) {
        MarketplaceProfessionalEntity professional = loadProfessional(userId);
        MarketplaceListingEntity listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing nao encontrado."));
        if (!listing.getProfessionalId().equals(professional.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Listing pertence a outro profissional.");
        }
        if (listing.getStatus() != ListingStatus.DRAFT && listing.getStatus() != ListingStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing nao esta em estado editavel.");
        }

        if (request.title() != null && !request.title().isBlank()) {
            listing.setTitle(request.title().trim());
        }
        if (request.description() != null && !request.description().isBlank()) {
            listing.setDescription(request.description().trim());
        }
        if (request.category() != null) {
            listing.setCategory(request.category());
        }
        if (request.priceCents() != null) {
            listing.setPriceCents(request.priceCents());
        }
        if (request.previewNodeIds() != null) {
            listing.setPreviewNodeIds(cleanPreviewNodes(request.previewNodeIds()));
        }
        listing.setRejectionReason(null);
        return new CreateListingResponse(listing.getId(), listing.getStatus());
    }

    @Transactional(readOnly = true)
    /**
     * Consulta o catálogo público de testes disponíveis para compra.
     *
     * <p>O resultado representa a vitrine do marketplace, com filtros para facilitar a busca
     * do cliente por tipo de teste, faixa de preço, reputação e texto.</p>
     */
    public MarketplacePageResponse<ListingSummaryResponse> search(ListingSearchFilter filter) {
        Pageable pageable = PageRequest.of(
                Math.max(filter.page(), 0),
                Math.min(Math.max(filter.size(), 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<ListingSummaryResponse> result = listingRepository.search(
                ListingStatus.APPROVED,
                filter.category(),
                filter.minPriceCents(),
                filter.maxPriceCents(),
                filter.minRating(),
                blankToNull(filter.text()),
                pageable
        ).map(this::toSummary);

        return new MarketplacePageResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    /**
     * Exibe a ficha completa de um item aprovado e disponível no marketplace.
     *
     * <p>Essa consulta entrega os dados necessários para o cliente entender o que está
     * comprando, quem é o profissional e quais trechos de prévia estão liberados.</p>
     */
    public ListingDetailResponse detail(Long id) {
        MarketplaceListingEntity listing = listingRepository.findByIdAndStatus(id, ListingStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing nao encontrado."));
        MarketplaceProfessionalEntity professional = loadProfessionalById(listing.getProfessionalId());
        return new ListingDetailResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getCategory(),
                listing.getPriceCents(),
                listing.getStatus(),
                listing.getSourceSimulationId(),
                listing.getSourceVersionId(),
                Set.copyOf(listing.getPreviewNodeIds()),
                previewNodes(listing),
                toProfessionalSummary(professional)
        );
    }

    private ListingSummaryResponse toSummary(MarketplaceListingEntity listing) {
        MarketplaceProfessionalEntity professional = loadProfessionalById(listing.getProfessionalId());
        return new ListingSummaryResponse(
                listing.getId(),
                listing.getTitle(),
                listing.getCategory(),
                listing.getPriceCents(),
                listing.getStatus(),
                professional.getAverageRating(),
                professional.getTotalReviews(),
                toProfessionalSummary(professional)
        );
    }

    private List<ListingDetailResponse.PreviewNodeResponse> previewNodes(MarketplaceListingEntity listing) {
        if (listing.getPreviewNodeIds().isEmpty()) {
            return List.of();
        }
        SimulationVersionEntity version = simulationVersionRepository.findById(listing.getSourceVersionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versao de simulacao nao encontrada."));
        Set<Long> allowedNodeIds = listing.getPreviewNodeIds();
        return version.getNodes().stream()
                .filter(node -> allowedNodeIds.contains(node.getId()))
                .sorted(Comparator.comparingInt(SimulationNodeEntity::getTurnIndex))
                .map(this::toPreviewNode)
                .toList();
    }

    private ListingDetailResponse.PreviewNodeResponse toPreviewNode(SimulationNodeEntity node) {
        List<ListingDetailResponse.PreviewOptionResponse> options = node.getOptions().stream()
                .sorted(Comparator.comparing(SimulationOptionEntity::getOptionId))
                .map(option -> new ListingDetailResponse.PreviewOptionResponse(
                        option.getId(),
                        option.getOptionId(),
                        option.getText()
                ))
                .toList();
        return new ListingDetailResponse.PreviewNodeResponse(
                node.getId(),
                node.getNodeId(),
                node.getTurnIndex(),
                node.getSpeaker(),
                node.getMessage(),
                options
        );
    }

    private MarketplaceProfessionalEntity loadProfessional(String userId) {
        Long id;
        try {
            id = Long.valueOf(userId);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessao invalida.");
        }
        return professionalRepository.findByUserId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado."));
    }

    private MarketplaceProfessionalEntity loadProfessionalById(Long professionalId) {
        return professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado."));
    }

    private ListingSummaryResponse.ProfessionalSummaryResponse toProfessionalSummary(
            MarketplaceProfessionalEntity professional
    ) {
        return new ListingSummaryResponse.ProfessionalSummaryResponse(
                professional.getId(),
                professional.getDisplayName(),
                professional.getVerificationStatus() == ProfessionalVerificationStatus.VERIFIED
        );
    }

    private static Set<Long> cleanPreviewNodes(Set<Long> previewNodeIds) {
        Set<Long> clean = new LinkedHashSet<>();
        if (previewNodeIds == null) {
            return clean;
        }
        for (Long nodeId : previewNodeIds) {
            if (nodeId != null) {
                clean.add(nodeId);
            }
        }
        return clean;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
