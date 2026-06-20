package br.com.iforce.praxis.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolve o tenant autenticado do contexto de segurança para os fluxos administrativos.
 * Quando {@code praxis.security.enabled=false} (perfis de desenvolvimento e testes), devolve o
 * tenant padrão configurado, mantendo os endpoints utilizáveis sem JWT.
 */
@Service
public class CurrentTenantService {

    private final boolean securityEnabled;
    private final String defaultTenantId;

    public CurrentTenantService(
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.default-tenant-id:tenant-1}") String defaultTenantId
    ) {
        this.securityEnabled = securityEnabled;
        this.defaultTenantId = defaultTenantId;
    }

    public String requiredTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object details = authentication == null ? null : authentication.getDetails();

        if (details instanceof AuthenticatedTenant authenticatedTenant
                && authenticatedTenant.tenantId() != null
                && !authenticatedTenant.tenantId().isBlank()) {
            return authenticatedTenant.tenantId();
        }

        if (details instanceof String tenantId && !tenantId.isBlank()) {
            return tenantId;
        }

        if (!securityEnabled) {
            return defaultTenantId;
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida ou expirada.");
    }

    public record AuthenticatedTenant(String tenantId) {
    }
}
