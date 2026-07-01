package br.com.iforce.praxis.marketplace.persistence.repository;

import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceMessageEntity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MarketplaceMessageRepository extends JpaRepository<MarketplaceMessageEntity, Long> {

    List<MarketplaceMessageEntity> findByThreadIdOrderByCreatedAtAsc(Long threadId);

    long countByThreadIdAndCreatedAtAfter(Long threadId, Instant createdAt);
}
