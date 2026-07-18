package br.com.iforce.praxis.auth.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PartnerSpecialistAuthorizationFilter extends OncePerRequestFilter {

    private static final String SPECIALIST_AUTHORITY = "ROLE_PARTNER_SPECIALIST";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities().stream()
                .noneMatch(authority -> SPECIALIST_AUTHORITY.equals(authority.getAuthority()))) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = resolveRequestPath(request);
        if (isAllowed(request.getMethod(), path)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"Especialistas podem criar e revisar avaliações, mas não administrar clientes, integrações, resultados ou cobrança.\"}");
    }

    private boolean isAllowed(String method, String path) {
        if (matchesPathOrDescendant(path, "/api/v1/account")) {
            return true;
        }
        if (matchesPathOrDescendant(path, "/api/v1/media")) {
            return true;
        }
        if (matchesPathOrDescendant(path, "/api/v1/empresa-config")) {
            return HttpMethod.GET.matches(method);
        }
        if (!matchesPathOrDescendant(path, "/api/v1/simulations")) {
            return false;
        }

        return !path.endsWith("/publish")
                && !path.endsWith("/clone-draft")
                && !path.endsWith("/archive")
                && !path.contains("/monitoring")
                && !path.contains("/calibration")
                && !path.contains("/talent-match")
                && !path.contains("/gupy-preflight");
    }

    private String resolveRequestPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        return contextPath == null || contextPath.isBlank()
                ? requestUri
                : requestUri.substring(contextPath.length());
    }

    private boolean matchesPathOrDescendant(String path, String rootPath) {
        return rootPath.equals(path) || path.startsWith(rootPath + "/");
    }
}
