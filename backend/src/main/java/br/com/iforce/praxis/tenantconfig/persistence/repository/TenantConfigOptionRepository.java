package br.com.iforce.praxis.tenantconfig.persistence.repository;

import br.com.iforce.praxis.tenantconfig.model.TenantConfigType;
import br.com.iforce.praxis.tenantconfig.persistence.entity.TenantConfigOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenantConfigOptionRepository extends JpaRepository<TenantConfigOptionEntity, Long> {

    List<TenantConfigOptionEntity> findByTenantIdAndConfigTypeOrderByDisplayOrderAscIdAsc(
            String tenantId,
            TenantConfigType configType
    );

    void deleteByTenantIdAndConfigType(String tenantId, TenantConfigType configType);
}
