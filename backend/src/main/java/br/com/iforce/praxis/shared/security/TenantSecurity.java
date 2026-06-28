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
            throw new IllegalStateException("Empresa obrigatória não foi estabelecida no contexto");
        }
        return tenant;
    }

    public static void validateTenantAccess(String resourceTenantId) {
        String currentTenant = requiredTenant();
        if (!currentTenant.equals(resourceTenantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não tem acesso a este item.");
        }
    }

    public static void validateTenantAccess(String resourceTenantId, String resourceId) {
        String currentTenant = requiredTenant();
        if (!currentTenant.equals(resourceTenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item não encontrado.");
        }
    }
}
