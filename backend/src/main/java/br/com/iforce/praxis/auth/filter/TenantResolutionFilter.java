package br.com.iforce.praxis.auth.filter;

import br.com.iforce.praxis.auth.context.TenantContextHolder;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final CurrentTenantService currentTenantService;
    private final JwtService jwtService;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final TenantRepository tenantRepository;
    private final boolean securityEnabled;
    private final String defaultTenantId;

    public TenantResolutionFilter(
            CurrentTenantService currentTenantService,
            JwtService jwtService,
            CandidateAttemptRepository candidateAttemptRepository,
            TenantRepository tenantRepository,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.default-tenant-id:tenant-1}") String defaultTenantId
    ) {
        this.currentTenantService = currentTenantService;
        this.jwtService = jwtService;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.tenantRepository = tenantRepository;
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
            // O ADMIN opera sobre tenants escolhidos explicitamente na rota e o webhook externo
            // não possui JWT de usuário: nenhum dos dois deve resolver/exigir tenant atual.
            if (isTenantExemptRequest(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (isPublicCandidateRequest(request)) {
                TenantContextHolder.set(resolvePublicCandidateTenant(request));
            } else if (!securityEnabled || (authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken))) {
                String tenantId = currentTenantService.requiredTenantId();
                if (isTenantBlocked(tenantId)) {
                    response.sendError(
                            HttpStatus.FORBIDDEN.value(),
                            "Cliente suspenso ou cancelado. Acesso bloqueado.");
                    return;
                }
                TenantContextHolder.set(tenantId);
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    /**
     * Verifica se o tenant está suspenso ou cancelado. Um cliente nesse estado não pode consumir
     * APIs protegidas, mesmo que já possua um token válido. Quando o tenant não existe no banco,
     * a resolução é deixada para as camadas seguintes (não bloqueia aqui).
     */
    private boolean isTenantBlocked(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return false;
        }
        return tenantRepository.findById(tenantId)
                .map(tenant -> tenant.getStatus() != null && tenant.getStatus().blocksAccess())
                .orElse(false);
    }

    private boolean isTenantExemptRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/admin/") || uri.startsWith("/api/webhooks/mercado-pago");
    }

    private boolean isPublicCandidateRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/candidate/");
    }

    private String resolvePublicCandidateTenant(HttpServletRequest request) {
        if (!securityEnabled) {
            return defaultTenantId;
        }

        String token = extractCandidateAttemptToken(request);
        if (token == null || token.isBlank()) {
            return defaultTenantId;
        }
        try {
            return jwtService.parseCandidateAttemptToken(token).tenantId();
        } catch (IllegalArgumentException | JwtException exception) {
            if (isLegacyAttemptId(token)) {
                return candidateAttemptRepository.findById(token)
                        .map(candidateAttempt -> candidateAttempt.getTenantId())
                        .orElse(defaultTenantId);
            }
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Token de tentativa do candidato invalido."
            );
        }
    }

    private static boolean isLegacyAttemptId(String value) {
        return value != null && value.matches("att_[A-Za-z0-9]{16,64}");
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
