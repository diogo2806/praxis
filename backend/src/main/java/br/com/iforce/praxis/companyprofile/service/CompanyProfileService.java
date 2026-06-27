package br.com.iforce.praxis.companyprofile.service;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.companyprofile.dto.CompanyProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Fornece os dados cadastrais da empresa (tenant) que está logada.
 *
 * <p>Na visão do processo, é aqui que o sistema busca as informações de
 * cadastro da própria empresa — nome, razão social, CNPJ, contato e site —
 * para exibir na tela de perfil. Trabalha sempre no contexto da empresa
 * autenticada, sem expor dados de outras empresas.</p>
 */
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

    /**
     * Devolve o perfil cadastral da empresa logada para exibir na tela.
     *
     * <p>Usa o nome fantasia quando disponível e, na falta dele, o nome
     * comum. É apenas consulta, não altera nada.</p>
     *
     * @return os dados cadastrais da empresa atual
     */
    @Transactional(readOnly = true)
    public CompanyProfileResponse getProfile() {
        String tenantId = currentTenantService.requiredTenantId();
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Empresa não encontrada."));

        return new CompanyProfileResponse(
                fallback(tenant.getTradeName(), tenant.getName()),
                tenant.getLegalName(),
                tenant.getTaxId(),
                tenant.getCorporateEmail(),
                tenant.getPhone(),
                tenant.getWebsite()
        );
    }

    /** Usa o valor preferido quando preenchido; senão, recorre a uma alternativa. Uso interno. */
    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
