package br.com.iforce.praxis.auth.filter;

import br.com.iforce.praxis.auth.context.TenantContextHolder;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final CurrentTenantService currentTenantService;

    public TenantResolutionFilter(CurrentTenantService currentTenantService) {
        this.currentTenantService = currentTenantService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.isAuthenticated()) {
                String tenantId = currentTenantService.requiredTenantId();
                TenantContextHolder.set(tenantId);
            }

            filterChain.doFilter(request, response);

        } finally {
            TenantContextHolder.clear();
        }
    }
}
