package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.marketplace.dto.CheckoutRequest;
import br.com.iforce.praxis.marketplace.dto.CheckoutResponse;
import br.com.iforce.praxis.marketplace.dto.MarketplaceOrderResponse;
import br.com.iforce.praxis.marketplace.model.ListingStatus;
import br.com.iforce.praxis.marketplace.model.OrderStatus;
import br.com.iforce.praxis.marketplace.model.PayoutStatus;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceListingEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceOrderEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplacePayoutEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceListingRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceOrderRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplacePayoutRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;
import br.com.iforce.praxis.simulation.dto.CloneSimulationVersionResponse;
import br.com.iforce.praxis.shared.notification.model.InAppNotificationType;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class MarketplaceOrderService {

    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceProfessionalRepository professionalRepository;
    private final MarketplaceOrderRepository orderRepository;
    private final MarketplacePayoutRepository payoutRepository;
    private final MarketplaceMercadoPagoService mercadoPagoService;
    private final MarketplaceListingCloneService listingCloneService;
    private final JdbcTemplate jdbcTemplate;
    private final MarketplaceTokenCryptoService tokenCryptoService;
    private final MarketplaceNotificationService notificationService;

    public MarketplaceOrderService(
            MarketplaceListingRepository listingRepository,
            MarketplaceProfessionalRepository professionalRepository,
            MarketplaceOrderRepository orderRepository,
            MarketplacePayoutRepository payoutRepository,
            MarketplaceMercadoPagoService mercadoPagoService,
            MarketplaceListingCloneService listingCloneService,
            JdbcTemplate jdbcTemplate,
            MarketplaceTokenCryptoService tokenCryptoService,
            MarketplaceNotificationService notificationService
    ) {
        this.listingRepository = listingRepository;
        this.professionalRepository = professionalRepository;
        this.orderRepository = orderRepository;
        this.payoutRepository = payoutRepository;
        this.mercadoPagoService = mercadoPagoService;
        this.listingCloneService = listingCloneService;
        this.jdbcTemplate = jdbcTemplate;
        this.tokenCryptoService = tokenCryptoService;
        this.notificationService = notificationService;
    }

    @Transactional
    public CheckoutResponse checkout(String buyerTenantId, CheckoutRequest request) {
        MarketplaceListingEntity listing = listingRepository.findByIdAndStatus(request.listingId(), ListingStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing nao encontrado."));
        MarketplaceProfessionalEntity professional = professionalRepository.findById(listing.getProfessionalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado."));
        if (professional.getMpSellerId() == null || professional.getMpSellerId().isBlank()
                || professional.getMpAccessToken() == null || professional.getMpAccessToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Profissional ainda nao conectou Mercado Pago.");
        }

        long platformFeeCents = calculatePlatformFee(listing.getPriceCents());
        MarketplaceOrderEntity order = new MarketplaceOrderEntity();
        order.setListingId(listing.getId());
        order.setBuyerTenantId(buyerTenantId);
        order.setProfessionalId(professional.getId());
        order.setPriceCents(listing.getPriceCents());
        order.setPlatformFeeCents(platformFeeCents);
        order.setProfessionalPayoutCents(listing.getPriceCents() - platformFeeCents);
        order = orderRepository.save(order);

        MarketplaceMercadoPagoService.CheckoutPreference preference =
                mercadoPagoService.createCheckoutPreference(
                        order,
                        listing,
                        professional,
                        tokenCryptoService.decrypt(professional.getMpAccessToken())
                );
        order.setMpPreferenceId(preference.preferenceId());
        return new CheckoutResponse(order.getId(), preference.checkoutUrl(), preference.preferenceId());
    }

    @Transactional(readOnly = true)
    public MarketplaceOrderResponse getOrder(String buyerTenantId, Long orderId) {
        MarketplaceOrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado."));
        if (!order.getBuyerTenantId().equals(buyerTenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Pedido pertence a outro tenant.");
        }
        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceOrderResponse> listOrders(String buyerTenantId) {
        return orderRepository.findByBuyerTenantIdOrderByCreatedAtDesc(buyerTenantId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void processApprovedPayment(JsonNode payment, String requestId) {
        Long orderId = orderId(payment);
        if (orderId == null) {
            return;
        }
        MarketplaceOrderEntity order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido marketplace nao encontrado."));
        if (order.getStatus() == OrderStatus.PAID) {
            return;
        }
        String status = text(payment, "status");
        if (!"approved".equals(status)) {
            return;
        }

        MarketplaceListingEntity listing = requireListing(order.getListingId());
        CloneSimulationVersionResponse clone = listingCloneService.cloneListingToTenant(listing, order.getBuyerTenantId());
        order.setStatus(OrderStatus.PAID);
        order.setMpPaymentId(text(payment, "id"));
        order.setClonedSimulationId(clone.simulationId());
        order.setPaidAt(Instant.now());

        MarketplacePayoutEntity payout = new MarketplacePayoutEntity();
        payout.setOrderId(order.getId());
        payout.setProfessionalId(order.getProfessionalId());
        payout.setAmountCents(order.getProfessionalPayoutCents());
        payout.setStatus(PayoutStatus.ESCROW);
        payout.setEscrowReleaseAt(Instant.now().plus(configLong("escrow_days", 7), ChronoUnit.DAYS));
        payoutRepository.save(payout);
        notificationService.notifyProfessional(
                order.getProfessionalId(),
                InAppNotificationType.MARKETPLACE_ORDER_RECEIVED,
                "Nova venda no marketplace",
                "O teste \"" + listing.getTitle() + "\" foi comprado e clonado para o cliente."
        );
    }

    private MarketplaceOrderResponse toResponse(MarketplaceOrderEntity order) {
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

    private MarketplaceListingEntity requireListing(Long listingId) {
        return listingRepository.findById(listingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing nao encontrado."));
    }

    private long calculatePlatformFee(long priceCents) {
        long percent = configLong("commission_percent", 20);
        return Math.round(priceCents * (percent / 100.0));
    }

    private long configLong(String key, long fallback) {
        List<String> values = jdbcTemplate.query(
                "SELECT config_value FROM marketplace_platform_config WHERE config_key = ?",
                (rs, rowNum) -> rs.getString("config_value"),
                key
        );
        if (values.isEmpty()) {
            return fallback;
        }
        try {
            return Long.parseLong(values.getFirst());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Long orderId(JsonNode payment) {
        JsonNode metadata = payment == null ? null : payment.get("metadata");
        JsonNode value = metadata == null ? null : metadata.get("marketplace_order_id");
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asLong();
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
