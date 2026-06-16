package br.com.iforce.praxis.auth.filter;

import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.gupy.service.GupyAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@Component
public class GupyApiKeyFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final GupyAuthService gupyAuthService;

    public GupyApiKeyFilter(GupyAuthService gupyAuthService) {
        this.gupyAuthService = gupyAuthService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals("/test") || path.startsWith("/test/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            try {
                GupyAuthService.GupyTenantContext context = gupyAuthService.validateBearerToken(authorization);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        "gupy",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_GUPY"))
                );
                authentication.setDetails(new CurrentTenantService.AuthenticatedTenant(context.tenantId()));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ResponseStatusException exception) {
                // Token invalido: segue sem autenticar; o controller devolve o erro adequado.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
