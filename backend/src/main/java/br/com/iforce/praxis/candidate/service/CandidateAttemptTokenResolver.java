package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.auth.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CandidateAttemptTokenResolver {

    private final JwtService jwtService;
    private final boolean securityEnabled;

    public CandidateAttemptTokenResolver(
            JwtService jwtService,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled
    ) {
        this.jwtService = jwtService;
        this.securityEnabled = securityEnabled;
    }

    public ResolvedAttemptToken resolve(String token) {
        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token da participação inválido.");
        }
        try {
            JwtService.CandidateAttemptToken parsed = jwtService.parseCandidateAttemptToken(token);
            return new ResolvedAttemptToken(parsed.empresaId(), parsed.attemptId());
        } catch (RuntimeException exception) {
            if (!securityEnabled && token.matches("att_[A-Za-z0-9]{16,64}")) {
                return new ResolvedAttemptToken(null, token);
            }
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token da participação inválido ou expirado.");
        }
    }

    public record ResolvedAttemptToken(String empresaId, String attemptId) {
    }
}
