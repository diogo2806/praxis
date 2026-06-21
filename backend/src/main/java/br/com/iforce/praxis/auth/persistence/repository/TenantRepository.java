package br.com.iforce.praxis.auth.persistence.repository;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<TenantEntity, String> {

    Optional<TenantEntity> findFirstByCompanyId(String companyId);

    Optional<TenantEntity> findFirstByIntegrationTokenHash(String integrationTokenHash);

    Optional<TenantEntity> findFirstByRecruteiIntegrationTokenHash(String recruteiIntegrationTokenHash);
}
