package br.com.iforce.praxis.companyprofile.service;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.companyprofile.dto.CompanyProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class CompanyProfileService {

    private final CurrentTenantService currentTenantService;
    private final TenantRepository tenantRepository;

    public CompanyProfileService(
            CurrentTenantService currentTenantService,
            TenantRepository tenantRepository
    ) {
        this.currentTenantService = currentTenantService;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public CompanyProfileResponse getProfile() {
        String tenantId = currentTenantService.requiredTenantId();
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Empresa nao encontrada."));

        return new CompanyProfileResponse(
                fallback(tenant.getTradeName(), tenant.getName()),
                tenant.getLegalName(),
                tenant.getTaxId(),
                tenant.getCorporateEmail(),
                tenant.getPhone(),
                tenant.getWebsite()
        );
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
