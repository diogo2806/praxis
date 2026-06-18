package br.com.iforce.praxis.tenantconfig.persistence.repository;

import br.com.iforce.praxis.tenantconfig.model.TenantConfigType;
import br.com.iforce.praxis.tenantconfig.persistence.entity.TenantConfigOptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantConfigOptionRepository extends JpaRepository<TenantConfigOptionEntity, Long> {

    List<TenantConfigOptionEntity> findByTenantIdAndConfigTypeOrderByDisplayOrderAscIdAsc(
            String tenantId,
            TenantConfigType configType
    );

    Optional<TenantConfigOptionEntity> findByTenantIdAndConfigTypeAndOptionValue(
            String tenantId,
            TenantConfigType configType,
            String optionValue
    );

    void deleteByTenantIdAndConfigType(String tenantId, TenantConfigType configType);
}
