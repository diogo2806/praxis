package br.com.iforce.praxis.auth.filter;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.auth.service.JwtService;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyAttemptRepository;

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
public class EmpresaResolutionFilter extends OncePerRequestFilter {

    private final CurrentEmpresaService currentEmpresaService;
    private final JwtService jwtService;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AssessmentJourneyAttemptRepository assessmentJourneyAttemptRepository;
    private final EmpresaRepository empresaRepository;
    private final boolean securityEnabled;
    private final String defaultEmpresaId;

    public EmpresaResolutionFilter(
            CurrentEmpresaService currentEmpresaService,
            JwtService jwtService,
            CandidateAttemptRepository candidateAttemptRepository,
            AssessmentJourneyAttemptRepository assessmentJourneyAttemptRepository,
            EmpresaRepository empresaRepository,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.default-empresa-id:empresa-1}") String defaultEmpresaId
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.jwtService = jwtService;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.assessmentJourneyAttemptRepository = assessmentJourneyAttemptRepository;
        this.empresaRepository = empresaRepository;
        this.securityEnabled = securityEnabled;
        this.defaultEmpresaId = defaultEmpresaId;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // O ADMIN opera sobre empresas escolhidos explicitamente na rota e o webhook externo
            // não possui JWT de usuário: nenhum dos dois deve resolver/exigir empresa atual.
            if (isEmpresaExemptRequest(request)) {
                filterChain.doFilter(request, response);
                return;
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (isPublicCandidateRequest(request)) {
                String publicEmpresaId = resolvePublicCandidateEmpresa(request);
                if (isEmpresaBlocked(publicEmpresaId)) {
                    response.sendError(
                            HttpStatus.FORBIDDEN.value(),
                            "Cliente suspenso ou cancelado. Acesso bloqueado.");
                    return;
                }
                EmpresaContextHolder.set(publicEmpresaId);
            } else if (!securityEnabled || (authentication != null
                    && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken))) {
                String empresaId = currentEmpresaService.requiredEmpresaId();
                if (isEmpresaBlocked(empresaId)) {
                    response.sendError(
                            HttpStatus.FORBIDDEN.value(),
                            "Cliente suspenso ou cancelado. Acesso bloqueado.");
                    return;
                }
                EmpresaContextHolder.set(empresaId);
            }

            filterChain.doFilter(request, response);
        } finally {
            EmpresaContextHolder.clear();
        }
    }

    /**
     * Verifica se o empresa está suspenso ou cancelado. Um cliente nesse estado não pode consumir
     * APIs protegidas, mesmo que já possua um token válido. Quando o empresa não existe no banco,
     * a resolução é deixada para as camadas seguintes (não bloqueia aqui).
     */
    private boolean isEmpresaBlocked(String empresaId) {
        if (empresaId == null || empresaId.isBlank()) {
            return false;
        }
        return empresaRepository.findById(empresaId)
                .map(empresa -> empresa.getStatus() != null && empresa.getStatus().blocksAccess())
                .orElse(false);
    }

    private boolean isEmpresaExemptRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/admin/") || uri.startsWith("/api/webhooks/mercado-pago");
    }

    private boolean isPublicCandidateRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/candidate/");
    }

    private String resolvePublicCandidateEmpresa(HttpServletRequest request) {
        if (!securityEnabled) {
            return defaultEmpresaId;
        }

        if (request.getRequestURI().startsWith("/candidate/journey-attempts/")) {
            String journeyAttemptId = extractJourneyAttemptId(request);
            if (journeyAttemptId == null || journeyAttemptId.isBlank()) {
                return defaultEmpresaId;
            }
            return assessmentJourneyAttemptRepository.findById(journeyAttemptId)
                    .map(journeyAttempt -> journeyAttempt.getEmpresaId())
                    .orElse(defaultEmpresaId);
        }

        String token = extractCandidateAttemptToken(request);
        if (token == null || token.isBlank()) {
            return defaultEmpresaId;
        }
        try {
            return jwtService.parseCandidateAttemptToken(token).empresaId();
        } catch (IllegalArgumentException | JwtException exception) {
            if (isLegacyAttemptId(token)) {
                return candidateAttemptRepository.findById(token)
                        .map(candidateAttempt -> candidateAttempt.getEmpresaId())
                        .orElse(defaultEmpresaId);
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

    private String extractJourneyAttemptId(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/candidate/journey-attempts/";
        if (!uri.startsWith(prefix)) {
            return null;
        }
        String remaining = uri.substring(prefix.length());
        int nextSlash = remaining.indexOf('/');
        return nextSlash >= 0 ? remaining.substring(0, nextSlash) : remaining;
    }
}
