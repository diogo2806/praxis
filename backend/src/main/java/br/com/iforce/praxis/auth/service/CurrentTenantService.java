package br.com.iforce.praxis.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Obtém qual empresa o usuário autenticado está usando no momento.
 *
 * O sistema é multi-tenant: cada empresa tem dados isolados. Este serviço
 * descobre qual empresa o usuário logado representa, extraindo a informação
 * do token JWT (em produção) ou de uma configuração padrão (em desenvolvimento).
 *
 * Isso garante que um usuário nunca consegue acessar dados de outra empresa,
 * mesmo que tente manipular requisições.
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

    /**
     * Obtém o ID da empresa do usuário autenticado.
     *
     * Busca a informação da empresa na sessão de segurança atual. Se o usuário
     * não estiver autenticado ou a informação estiver faltando, lança uma exceção.
     *
     * Em ambiente de desenvolvimento/testes (quando segurança está desativada),
     * retorna a empresa padrão configurada para permitir testes sem autenticação.
     *
     * @return ID da empresa do usuário autenticado
     * @throws ResponseStatusException se o usuário não está autenticado ou a sessão expirou
     */
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
