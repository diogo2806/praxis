package br.com.iforce.praxis.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {

    private static final String CANDIDATE_ATTEMPT_TOKEN_TYPE = "candidate_attempt";

    private final SecretKey secretKey;
    private final int expirationHours;

    public JwtService(
            @Value("${praxis.jwt-secret}") String secret,
            @Value("${praxis.jwt-expiration-hours:8}") int expirationHours,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled
    ) {
        String effectiveSecret = secret;
        if (securityEnabled && (secret == null
                || secret.isBlank()
                || secret.length() < 32
                || "dev-praxis-jwt-secret-2026-32-characters-minimum".equals(secret))) {
            throw new IllegalStateException("praxis.jwt-secret deve ser configurado com segredo forte fora do valor de desenvolvimento.");
        }
        if (!securityEnabled && (effectiveSecret == null || effectiveSecret.isBlank())) {
            effectiveSecret = "dev-praxis-jwt-secret-2026-32-characters-minimum";
        }
        this.secretKey = Keys.hmacShaKeyFor(effectiveSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationHours = expirationHours;
    }

    public String generateToken(String userId, String tenantId, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("tenant_id", tenantId)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationHours * 60L * 60L)))
                .signWith(secretKey)
                .compact();
    }

    public String generateCandidateAttemptToken(String tenantId, String attemptId, int ttlHours) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(attemptId)
                .claim("typ", CANDIDATE_ATTEMPT_TOKEN_TYPE)
                .claim("tenant_id", tenantId)
                .claim("attempt_id", attemptId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlHours * 60L * 60L)))
                .signWith(secretKey)
                .compact();
    }

    public CandidateAttemptToken parseCandidateAttemptToken(String token) {
        Claims claims = parse(token);
        if (!CANDIDATE_ATTEMPT_TOKEN_TYPE.equals(claims.get("typ", String.class))) {
            throw new IllegalArgumentException("Token público de candidato inválido.");
        }
        String tenantId = claims.get("tenant_id", String.class);
        String attemptId = claims.get("attempt_id", String.class);
        if (tenantId == null || tenantId.isBlank() || attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("Token publico de candidato incompleto.");
        }
        return new CandidateAttemptToken(tenantId, attemptId);
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public record CandidateAttemptToken(String tenantId, String attemptId) {
    }
}
