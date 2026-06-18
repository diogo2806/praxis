package br.com.iforce.praxis.auth.filter;

import br.com.iforce.praxis.auth.context.TenantContextHolder;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final CurrentTenantService currentTenantService;
    private final boolean securityEnabled;
    private final String defaultTenantId;

    public TenantResolutionFilter(
            CurrentTenantService currentTenantService,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.default-tenant-id:tenant-1}") String defaultTenantId
    ) {
        this.currentTenantService = currentTenantService;
        this.securityEnabled = securityEnabled;
        this.defaultTenantId = defaultTenantId;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (isPublicCandidateRequest(request)) {
                TenantContextHolder.set(defaultTenantId);
            } else if (!securityEnabled || (authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken))) {
                String tenantId = currentTenantService.requiredTenantId();
                TenantContextHolder.set(tenantId);
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContextHolder.clear();
        }
    }

    private boolean isPublicCandidateRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/candidate/");
    }
}
