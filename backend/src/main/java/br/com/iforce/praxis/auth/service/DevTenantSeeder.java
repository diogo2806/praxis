package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public class DevTenantSeeder {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevTenantSeeder.class);
    private static final String DEFAULT_TENANT_NAME = "Acme S.A.";
    private static final String DEFAULT_COMPANY_ID = "empresa-123";

    private final TenantRepository tenantRepository;
    private final String defaultTenantId;

    public DevTenantSeeder(
            TenantRepository tenantRepository,
            @Value("${praxis.default-tenant-id:tenant-1}") String defaultTenantId
    ) {
        this.tenantRepository = tenantRepository;
        this.defaultTenantId = defaultTenantId;
    }

    @PostConstruct
    public void seedDefaultTenant() {
        if (tenantRepository.existsById(defaultTenantId)) {
            return;
        }

        TenantEntity tenant = new TenantEntity();
        tenant.setId(defaultTenantId);
        tenant.setName(DEFAULT_TENANT_NAME);
        tenant.setCompanyId(DEFAULT_COMPANY_ID);

        tenantRepository.save(tenant);
        LOGGER.info("Tenant padrao de desenvolvimento criado: {}", defaultTenantId);
    }
}
