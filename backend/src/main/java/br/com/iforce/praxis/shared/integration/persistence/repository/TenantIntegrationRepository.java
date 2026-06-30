package br.com.iforce.praxis.shared.integration.persistence.repository;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.persistence.entity.TenantIntegrationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantIntegrationRepository extends JpaRepository<TenantIntegrationEntity, Long> {

    List<TenantIntegrationEntity> findByTenantIdOrderByProviderAsc(String tenantId);

    Optional<TenantIntegrationEntity> findFirstByTenantIdAndProvider(
            String tenantId,
            IntegrationProvider provider
    );
}
