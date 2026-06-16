package br.com.iforce.praxis.auth.persistence.repository;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<TenantEntity, String> {
}
