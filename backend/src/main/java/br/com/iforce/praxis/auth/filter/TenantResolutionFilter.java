package br.com.iforce.praxis.auth.filter;

import br.com.iforce.praxis.auth.context.TenantContextHolder;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.auth.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final CurrentTenantService currentTenantService;
    private final JwtService jwtService;
    private final boolean securityEnabled;
    private final String defaultTenantId;

    public TenantResolutionFilter(
            CurrentTenantService currentTenantService,
            JwtService jwtService,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.default-tenant-id:tenant-1}") String defaultTenantId
    ) {
        this.currentTenantService = currentTenantService;
        this.jwtService = jwtService;
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
                TenantContextHolder.set(resolvePublicCandidateTenant(request));
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

    private String resolvePublicCandidateTenant(HttpServletRequest request) {
        String token = extractCandidateAttemptToken(request);
        if (token == null || token.isBlank()) {
            return defaultTenantId;
        }
        try {
            return jwtService.parseCandidateAttemptToken(token).tenantId();
        } catch (IllegalArgumentException | JwtException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token de tentativa do candidato invalido."
            );
        }
    }

    private String extractCandidateAttemptToken(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/candidate/attempts/";
        if (!uri.startsWith(prefix)) {
            return null;
        }
        String remaining = uri.substring(prefix.length());
        int nextSlash = remaining.indexOf('/');
        return nextSlash >= 0 ? remaining.substring(0, nextSlash) : remaining;
    }
}
