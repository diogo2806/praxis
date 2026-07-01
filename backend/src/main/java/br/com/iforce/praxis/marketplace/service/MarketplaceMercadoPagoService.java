package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.billing.service.MercadoPagoClient;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceListingEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceOrderEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MarketplaceMercadoPagoService {

    private final MercadoPagoClient mercadoPagoClient;

    public MarketplaceMercadoPagoService(MercadoPagoClient mercadoPagoClient) {
        this.mercadoPagoClient = mercadoPagoClient;
    }

    public CheckoutPreference createCheckoutPreference(
            MarketplaceOrderEntity order,
            MarketplaceListingEntity listing,
            MarketplaceProfessionalEntity professional,
            String sellerAccessToken
    ) {
        String externalReference = "marketplace:" + order.getBuyerTenantId() + ":" + order.getId();
        JsonNode response = mercadoPagoClient.createSplitPreference(
                listing.getTitle(),
                listing.getPriceCents(),
                order.getPlatformFeeCents(),
                externalReference,
                Map.of(
                        "order_type", "marketplace",
                        "marketplace_order_id", order.getId(),
                        "buyer_tenant_id", order.getBuyerTenantId(),
                        "listing_id", listing.getId()
                ),
                sellerAccessToken
        );
        return new CheckoutPreference(text(response, "id"), text(response, "init_point"));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    public record CheckoutPreference(String preferenceId, String checkoutUrl) {
    }
}
