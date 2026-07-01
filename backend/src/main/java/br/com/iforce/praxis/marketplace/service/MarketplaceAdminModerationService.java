package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.marketplace.dto.AdminMarketplaceDashboardResponse;
import br.com.iforce.praxis.marketplace.dto.AdminModerateListingRequest;
import br.com.iforce.praxis.marketplace.dto.AdminModerateProfessionalRequest;
import br.com.iforce.praxis.marketplace.dto.AdminRefundOrderRequest;
import br.com.iforce.praxis.marketplace.dto.CreateListingResponse;
import br.com.iforce.praxis.marketplace.dto.MarketplaceOrderResponse;
import br.com.iforce.praxis.marketplace.dto.ProfessionalPublicProfileResponse;
import br.com.iforce.praxis.marketplace.model.ListingStatus;
import br.com.iforce.praxis.marketplace.model.OrderStatus;
import br.com.iforce.praxis.marketplace.model.PayoutStatus;
import br.com.iforce.praxis.marketplace.model.ProfessionalVerificationStatus;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceListingEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceOrderEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplacePayoutEntity;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceListingRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceOrderRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplacePayoutRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;
import br.com.iforce.praxis.shared.notification.model.InAppNotificationType;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Service
/**
 * Executa as decis&otilde;es administrativas que mant&ecirc;m a qualidade e a governan&ccedil;a do marketplace.
 *
 * <p>Este servi&ccedil;o representa a mesa de opera&ccedil;&atilde;o da plataforma: acompanha indicadores,
 * aprova ou bloqueia profissionais e itens, trata disputas e registra as a&ccedil;&otilde;es de auditoria.</p>
 */
public class MarketplaceAdminModerationService {

    private final MarketplaceProfessionalRepository professionalRepository;
    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceOrderRepository orderRepository;
    private final MarketplacePayoutRepository payoutRepository;
    private final AuditEventService auditEventService;
    private final MarketplaceNotificationService notificationService;

    public MarketplaceAdminModerationService(
            MarketplaceProfessionalRepository professionalRepository,
            MarketplaceListingRepository listingRepository,
            MarketplaceOrderRepository orderRepository,
            MarketplacePayoutRepository payoutRepository,
            AuditEventService auditEventService,
            MarketplaceNotificationService notificationService
    ) {
        this.professionalRepository = professionalRepository;
        this.listingRepository = listingRepository;
        this.orderRepository = orderRepository;
        this.payoutRepository = payoutRepository;
        this.auditEventService = auditEventService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    /**
     * Resume a situa&ccedil;&atilde;o operacional do marketplace para uso administrativo.
     *
     * <p>O objetivo &eacute; mostrar rapidamente quantos cadastros, publica&ccedil;&otilde;es e vendas exigem
     * acompanhamento da equipe da plataforma.</p>
     */
    public AdminMarketplaceDashboardResponse dashboard() {
        return new AdminMarketplaceDashboardResponse(
                professionalRepository.findByVerificationStatusOrderByCreatedAtAsc(
                        ProfessionalVerificationStatus.PENDING_VERIFICATION).size(),
                professionalRepository.findByVerificationStatusOrderByCreatedAtAsc(
                        ProfessionalVerificationStatus.VERIFIED).size(),
                listingRepository.findByStatusOrderByCreatedAtAsc(ListingStatus.PENDING_REVIEW).size(),
                listingRepository.findByStatusOrderByCreatedAtAsc(ListingStatus.APPROVED).size(),
                orderRepository.findByStatus(OrderStatus.PAID).size()
        );
    }

    @Transactional(readOnly = true)
    /**
     * Lista profissionais aguardando valida&ccedil;&atilde;o da plataforma.
     */
    public List<ProfessionalPublicProfileResponse> pendingProfessionals() {
        return professionalRepository.findByVerificationStatusOrderByCreatedAtAsc(
                        ProfessionalVerificationStatus.PENDING_VERIFICATION)
                .stream()
                .map(this::toProfessionalResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Lista profissionais conforme o status operacional desejado.
     */
    public List<ProfessionalPublicProfileResponse> professionalsByStatus(ProfessionalVerificationStatus status) {
        ProfessionalVerificationStatus effectiveStatus = status == null
                ? ProfessionalVerificationStatus.PENDING_VERIFICATION
                : status;
        return professionalRepository.findByVerificationStatusOrderByCreatedAtAsc(effectiveStatus)
                .stream()
                .map(this::toProfessionalResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Lista itens que foram enviados para revis&atilde;o e ainda aguardam decis&atilde;o.
     */
    public List<CreateListingResponse> pendingListings() {
        return listingRepository.findByStatusOrderByCreatedAtAsc(ListingStatus.PENDING_REVIEW)
                .stream()
                .map(listing -> new CreateListingResponse(listing.getId(), listing.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Lista itens do marketplace conforme o status administrativo informado.
     */
    public List<CreateListingResponse> listingsByStatus(ListingStatus status) {
        ListingStatus effectiveStatus = status == null ? ListingStatus.PENDING_REVIEW : status;
        return listingRepository.findByStatusOrderByCreatedAtAsc(effectiveStatus)
                .stream()
                .map(listing -> new CreateListingResponse(listing.getId(), listing.getStatus()))
                .toList();
    }

    @Transactional
    /**
     * Aprova o cadastro de um profissional para que ele possa atuar normalmente no marketplace.
     *
     * <p>Depois desta decis&atilde;o, o profissional passa a ter condi&ccedil;&otilde;es de submeter materiais
     * comerciais dentro do fluxo padr&atilde;o da plataforma.</p>
     */
    public ProfessionalPublicProfileResponse approveProfessional(
            String actorUserId,
            Long professionalId,
            AdminModerateProfessionalRequest request
    ) {
        MarketplaceProfessionalEntity professional = requireProfessional(professionalId);
        professional.setVerificationStatus(ProfessionalVerificationStatus.VERIFIED);
        audit(actorUserId, AuditEventType.MARKETPLACE_PROFESSIONAL_APPROVED, "Profissional marketplace aprovado.",
                "{\"professionalId\":" + professionalId + ",\"reason\":\"" + escape(reason(request)) + "\"}");
        notificationService.notifyProfessional(
                professionalId,
                InAppNotificationType.MARKETPLACE_PROFESSIONAL_VERIFIED,
                "Cadastro profissional aprovado",
                "Seu cadastro profissional foi verificado pela plataforma."
        );
        return toProfessionalResponse(professional);
    }

    @Transactional
    /**
     * Rejeita o cadastro de um profissional durante a etapa de valida&ccedil;&atilde;o.
     *
     * <p>Este caminho &eacute; usado quando a plataforma entende que o perfil n&atilde;o est&aacute; apto para
     * ingresso no marketplace nas condi&ccedil;&otilde;es atuais.</p>
     */
    public ProfessionalPublicProfileResponse rejectProfessional(
            String actorUserId,
            Long professionalId,
            AdminModerateProfessionalRequest request
    ) {
        MarketplaceProfessionalEntity professional = requireProfessional(professionalId);
        professional.setVerificationStatus(ProfessionalVerificationStatus.REJECTED);
        audit(actorUserId, AuditEventType.MARKETPLACE_PROFESSIONAL_REJECTED, "Profissional marketplace rejeitado.",
                "{\"professionalId\":" + professionalId + ",\"reason\":\"" + escape(reason(request)) + "\"}");
        return toProfessionalResponse(professional);
    }

    @Transactional
    /**
     * Suspende um profissional j&aacute; cadastrado, interrompendo sua opera&ccedil;&atilde;o no marketplace.
     *
     * <p>Serve para a&ccedil;&otilde;es corretivas ou preventivas quando a plataforma precisa limitar a atua&ccedil;&atilde;o
     * de um vendedor sem remover historicamente seus registros.</p>
     */
    public ProfessionalPublicProfileResponse suspendProfessional(
            String actorUserId,
            Long professionalId,
            AdminModerateProfessionalRequest request
    ) {
        MarketplaceProfessionalEntity professional = requireProfessional(professionalId);
        professional.setVerificationStatus(ProfessionalVerificationStatus.SUSPENDED);
        audit(actorUserId, AuditEventType.MARKETPLACE_PROFESSIONAL_SUSPENDED, "Profissional marketplace suspenso.",
                "{\"professionalId\":" + professionalId + ",\"reason\":\"" + escape(reason(request)) + "\"}");
        return toProfessionalResponse(professional);
    }

    @Transactional
    /**
     * Aprova um item para venda no marketplace.
     *
     * <p>Ap&oacute;s esta etapa, o teste deixa de ser apenas um material em revis&atilde;o e passa a compor
     * a vitrine p&uacute;blica dispon&iacute;vel para compra.</p>
     */
    public CreateListingResponse approveListing(String actorUserId, Long listingId, AdminModerateListingRequest request) {
        MarketplaceListingEntity listing = requireListing(listingId);
        listing.setStatus(ListingStatus.APPROVED);
        listing.setRejectionReason(null);
        audit(actorUserId, AuditEventType.MARKETPLACE_LISTING_APPROVED, "Listing marketplace aprovado.",
                "{\"listingId\":" + listingId + ",\"reason\":\"" + escape(reason(request)) + "\"}");
        notificationService.notifyProfessional(
                listing.getProfessionalId(),
                InAppNotificationType.MARKETPLACE_LISTING_APPROVED,
                "Teste aprovado no marketplace",
                "Seu teste \"" + listing.getTitle() + "\" foi aprovado para venda."
        );
        return new CreateListingResponse(listing.getId(), listing.getStatus());
    }

    @Transactional
    /**
     * Rejeita um item e devolve ao profissional a necessidade de corre&ccedil;&atilde;o.
     *
     * <p>A rejei&ccedil;&atilde;o registra o motivo operacional e permite que o autor ajuste o material
     * antes de tentar nova submiss&atilde;o.</p>
     */
    public CreateListingResponse rejectListing(String actorUserId, Long listingId, AdminModerateListingRequest request) {
        MarketplaceListingEntity listing = requireListing(listingId);
        listing.setStatus(ListingStatus.REJECTED);
        listing.setRejectionReason(reason(request));
        audit(actorUserId, AuditEventType.MARKETPLACE_LISTING_REJECTED, "Listing marketplace rejeitado.",
                "{\"listingId\":" + listingId + ",\"reason\":\"" + escape(reason(request)) + "\"}");
        notificationService.notifyProfessional(
                listing.getProfessionalId(),
                InAppNotificationType.MARKETPLACE_LISTING_REJECTED,
                "Teste rejeitado no marketplace",
                "Seu teste \"" + listing.getTitle() + "\" foi rejeitado: " + reason(request)
        );
        return new CreateListingResponse(listing.getId(), listing.getStatus());
    }

    @Transactional
    /**
     * Suspende um item previamente publicado ou em circula&ccedil;&atilde;o administrativa.
     *
     * <p>Esse fluxo remove o item da opera&ccedil;&atilde;o comercial quando a plataforma precisa interromper
     * sua disponibilidade por motivo de governan&ccedil;a, risco ou qualidade.</p>
     */
    public CreateListingResponse suspendListing(String actorUserId, Long listingId, AdminModerateListingRequest request) {
        MarketplaceListingEntity listing = requireListing(listingId);
        listing.setStatus(ListingStatus.SUSPENDED);
        listing.setRejectionReason(reason(request));
        audit(actorUserId, AuditEventType.MARKETPLACE_LISTING_SUSPENDED, "Listing marketplace suspenso.",
                "{\"listingId\":" + listingId + ",\"reason\":\"" + escape(reason(request)) + "\"}");
        return new CreateListingResponse(listing.getId(), listing.getStatus());
    }

    @Transactional(readOnly = true)
    /**
     * Lista pedidos que entraram em disputa e exigem interven&ccedil;&atilde;o humana.
     */
    public List<MarketplaceOrderResponse> disputedOrders() {
        return orderRepository.findByStatus(OrderStatus.DISPUTED)
                .stream()
                .map(this::toOrderResponse)
                .toList();
    }

    @Transactional
    /**
     * Registra o reembolso administrativo de uma compra do marketplace.
     *
     * <p>Esse processo encerra a venda, atualiza o pedido e ajusta o repasse associado para
     * refletir que o valor n&atilde;o deve mais seguir para o profissional.</p>
     */
    public MarketplaceOrderResponse refundOrder(String actorUserId, Long orderId, AdminRefundOrderRequest request) {
        MarketplaceOrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado."));
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.DISPUTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pedido nao esta elegivel para reembolso.");
        }
        order.setStatus(OrderStatus.REFUNDED);
        payoutRepository.findByOrderId(order.getId()).ifPresent(this::reversePayout);
        audit(actorUserId, AuditEventType.MARKETPLACE_ORDER_REFUNDED, "Pedido marketplace reembolsado.",
                "{\"orderId\":" + orderId + ",\"reason\":\"" + escape(reason(request)) + "\"}");
        return toOrderResponse(order);
    }

    private MarketplaceProfessionalEntity requireProfessional(Long professionalId) {
        return professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado."));
    }

    private MarketplaceListingEntity requireListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing nao encontrado."));
    }

    private void reversePayout(MarketplacePayoutEntity payout) {
        String detail = payout.getStatus() == PayoutStatus.RELEASED
                ? "Pedido reembolsado apos liberacao do repasse."
                : "Pedido reembolsado antes da liberacao.";
        payout.setStatus(PayoutStatus.REVERSED);
        payout.setFailureReason(detail);
    }

    private MarketplaceOrderResponse toOrderResponse(MarketplaceOrderEntity order) {
        MarketplaceListingEntity listing = requireListing(order.getListingId());
        return new MarketplaceOrderResponse(
                order.getId(),
                order.getStatus(),
                listing.getTitle(),
                order.getPriceCents(),
                order.getClonedSimulationId(),
                order.getPaidAt()
        );
    }

    private ProfessionalPublicProfileResponse toProfessionalResponse(MarketplaceProfessionalEntity professional) {
        return new ProfessionalPublicProfileResponse(
                professional.getId(),
                professional.getDisplayName(),
                professional.getBio(),
                Set.copyOf(professional.getSpecialties()),
                professional.getLinkedinUrl(),
                professional.getVerificationStatus(),
                professional.getAverageRating(),
                professional.getTotalReviews(),
                professional.getTotalSales(),
                professional.getMpSellerId() != null && !professional.getMpSellerId().isBlank()
        );
    }

    private void audit(String actorUserId, AuditEventType eventType, String message, String metadata) {
        auditEventService.auditAdminAction(actorUserId, AuditEventService.PLATFORM_EMPRESA_ID, eventType, message, metadata);
    }

    private static String reason(AdminModerateProfessionalRequest request) {
        return request == null || request.reason() == null ? "" : request.reason().trim();
    }

    private static String reason(AdminModerateListingRequest request) {
        return request == null || request.reason() == null ? "" : request.reason().trim();
    }

    private static String reason(AdminRefundOrderRequest request) {
        return request == null || request.reason() == null ? "" : request.reason().trim();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
