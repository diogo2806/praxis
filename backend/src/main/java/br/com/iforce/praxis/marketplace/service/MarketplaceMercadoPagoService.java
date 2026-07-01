package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.billing.service.MercadoPagoClient;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceListingEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceOrderEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
/**
 * Prepara a etapa financeira da compra no marketplace usando o Mercado Pago.
 *
 * <p>Na jornada do usu&aacute;rio, este servi&ccedil;o monta os dados que permitem abrir o checkout e dividir
 * corretamente o valor entre plataforma e profissional vendedor.</p>
 */
public class MarketplaceMercadoPagoService {

    private final MercadoPagoClient mercadoPagoClient;

    public MarketplaceMercadoPagoService(MercadoPagoClient mercadoPagoClient) {
        this.mercadoPagoClient = mercadoPagoClient;
    }

    /**
     * Cria a prefer&ecirc;ncia de pagamento usada para iniciar o checkout de uma compra do marketplace.
     *
     * <p>Esse passo registra no provedor de pagamento qual item est&aacute; sendo vendido, quem est&aacute;
     * comprando e como o valor ser&aacute; distribu&iacute;do quando a transa&ccedil;&atilde;o for conclu&iacute;da.</p>
     */
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

    /**
     * Dados necess&aacute;rios para abrir o checkout no frontend e reconciliar o pedido no backend.
     */
    public record CheckoutPreference(String preferenceId, String checkoutUrl) {
    }
}
