package br.com.iforce.praxis.marketplace.persistence.repository;

import br.com.iforce.praxis.marketplace.model.OrderStatus;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceOrderEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MarketplaceOrderRepository extends JpaRepository<MarketplaceOrderEntity, Long> {

    List<MarketplaceOrderEntity> findByBuyerTenantIdOrderByCreatedAtDesc(String buyerTenantId);

    List<MarketplaceOrderEntity> findByProfessionalIdOrderByCreatedAtDesc(Long professionalId);

    Optional<MarketplaceOrderEntity> findByMpPaymentId(String mpPaymentId);

    List<MarketplaceOrderEntity> findByStatus(OrderStatus status);
}
