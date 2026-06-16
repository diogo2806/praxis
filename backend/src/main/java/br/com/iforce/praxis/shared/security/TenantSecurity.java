package br.com.iforce.praxis.shared.security;

import br.com.iforce.praxis.auth.context.TenantContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class TenantSecurity {

    private TenantSecurity() {
    }

    public static String requiredTenant() {
        String tenant = TenantContextHolder.get();
        if (tenant == null) {
            throw new IllegalStateException("Tenant obrigatório não foi estabelecido no contexto");
        }
        return tenant;
    }

    public static void validateTenantAccess(String resourceTenantId) {
        String currentTenant = requiredTenant();
        if (!currentTenant.equals(resourceTenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado: recurso pertence a outro tenant");
        }
    }

    public static void validateTenantAccess(String resourceTenantId, String resourceId) {
        String currentTenant = requiredTenant();
        if (!currentTenant.equals(resourceTenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso não encontrado");
        }
    }
}
