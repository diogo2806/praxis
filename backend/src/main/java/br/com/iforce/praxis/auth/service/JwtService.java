package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

/**
 * Cria e valida tokens de segurança (JWT).
 */
@Service
public class JwtService {

    private static final String CANDIDATE_ATTEMPT_TOKEN_TYPE = "candidate_attempt";
    private static final String CANDIDATE_RESULT_TOKEN_TYPE = "candidate_result";

    private final SecretKey secretKey;
    private final int expirationHours;
    private final ObjectProvider<CandidateAttemptRepository> candidateAttemptRepositoryProvider;

    /** Construtor mantido para testes unitários isolados. */
    public JwtService(String secret, int expirationHours, boolean securityEnabled) {
        this(secret, expirationHours, securityEnabled, null);
    }

    @Autowired
    public JwtService(
            @Value("${praxis.jwt-secret}") String secret,
            @Value("${praxis.jwt-expiration-hours:8}") int expirationHours,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            ObjectProvider<CandidateAttemptRepository> candidateAttemptRepositoryProvider
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
        this.candidateAttemptRepositoryProvider = candidateAttemptRepositoryProvider;
    }

    public String generateToken(String userId, String empresaId, Set<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("empresa_id", empresaId)
                .claim("roles", roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expirationHours * 60L * 60L)))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Gera a credencial usando a janela canônica persistida na tentativa.
     * A emissão não renova a validade de forma implícita.
     */
    public String generateCandidateAttemptToken(String empresaId, String attemptId, int ttlHours) {
        if (ttlHours <= 0) {
            throw new IllegalArgumentException("A validade do token deve ser maior que zero.");
        }
        CandidateAttemptEntity attempt = requiredCandidateAttempt(attemptId);
        if (!empresaId.equals(attempt.getEmpresaId())) {
            throw new IllegalArgumentException("A tentativa não pertence à empresa informada.");
        }
        Instant issuedAt = requiredInstant(
                attempt.getCandidateTokenIssuedAt(),
                "O instante de emissão do token da tentativa não foi persistido."
        );
        Instant expiresAt = requiredInstant(
                attempt.getCandidateTokenExpiresAt(),
                "O instante de expiração do token da tentativa não foi persistido."
        );
        return generateCandidateAttemptToken(empresaId, attemptId, issuedAt, expiresAt);
    }

    /**
     * Gera um token com duração fixa a partir do instante informado.
     * Mantido para testes e consumidores que já carregaram o início da janela.
     */
    public String generateCandidateAttemptToken(
            String empresaId,
            String attemptId,
            int ttlHours,
            Instant issuedAt
    ) {
        if (ttlHours <= 0) {
            throw new IllegalArgumentException("A validade do token deve ser maior que zero.");
        }
        Instant normalizedIssuedAt = requiredInstant(issuedAt, "O instante de emissão da tentativa é obrigatório.");
        return generateCandidateAttemptToken(
                empresaId,
                attemptId,
                normalizedIssuedAt,
                normalizedIssuedAt.plusSeconds(ttlHours * 60L * 60L)
        );
    }

    /**
     * Gera o JWT usando exatamente a janela persistida ou explicitamente fornecida.
     */
    public String generateCandidateAttemptToken(
            String empresaId,
            String attemptId,
            Instant issuedAt,
            Instant expiresAt
    ) {
        Instant normalizedIssuedAt = requiredInstant(issuedAt, "O instante de emissão da tentativa é obrigatório.");
        Instant normalizedExpiresAt = requiredInstant(expiresAt, "O instante de expiração da tentativa é obrigatório.");
        if (!normalizedExpiresAt.isAfter(normalizedIssuedAt)) {
            throw new IllegalArgumentException("A expiração do token deve ser posterior à emissão.");
        }
        return generateCandidateScopedToken(
                empresaId,
                attemptId,
                CANDIDATE_ATTEMPT_TOKEN_TYPE,
                normalizedIssuedAt,
                normalizedExpiresAt
        );
    }

    public String generateCandidateResultToken(String empresaId, String attemptId, int ttlHours) {
        if (ttlHours <= 0) {
            throw new IllegalArgumentException("A validade do token deve ser maior que zero.");
        }
        Instant now = Instant.now();
        return generateCandidateScopedToken(
                empresaId,
                attemptId,
                CANDIDATE_RESULT_TOKEN_TYPE,
                now,
                now.plusSeconds(ttlHours * 60L * 60L)
        );
    }

    public CandidateAttemptToken parseCandidateAttemptToken(String token) {
        CandidateScopedToken parsed = parseCandidateScopedToken(token, CANDIDATE_ATTEMPT_TOKEN_TYPE);
        return new CandidateAttemptToken(parsed.empresaId(), parsed.attemptId());
    }

    public CandidateResultToken parseCandidateResultToken(String token) {
        CandidateScopedToken parsed = parseCandidateScopedToken(token, CANDIDATE_RESULT_TOKEN_TYPE);
        return new CandidateResultToken(parsed.empresaId(), parsed.attemptId());
    }

    private CandidateAttemptEntity requiredCandidateAttempt(String attemptId) {
        if (candidateAttemptRepositoryProvider == null) {
            throw new IllegalStateException("O repositório de tentativas é obrigatório para gerar o token persistido.");
        }
        CandidateAttemptRepository repository = candidateAttemptRepositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new IllegalStateException("O repositório de tentativas não está disponível.");
        }
        return repository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("Tentativa não encontrada para geração do token."));
    }

    private Instant requiredInstant(Instant value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private String generateCandidateScopedToken(
            String empresaId,
            String attemptId,
            String type,
            Instant issuedAt,
            Instant expiresAt
    ) {
        return Jwts.builder()
                .subject(attemptId)
                .claim("typ", type)
                .claim("empresa_id", empresaId)
                .claim("attempt_id", attemptId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    private CandidateScopedToken parseCandidateScopedToken(String token, String expectedType) {
        Claims claims = parse(token);
        if (!expectedType.equals(claims.get("typ", String.class))) {
            throw new IllegalArgumentException("Token público de candidato inválido.");
        }
        String empresaId = claims.get("empresa_id", String.class);
        String attemptId = claims.get("attempt_id", String.class);
        if (empresaId == null || empresaId.isBlank() || attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("Token público de candidato incompleto.");
        }
        return new CandidateScopedToken(empresaId, attemptId);
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private record CandidateScopedToken(String empresaId, String attemptId) {
    }

    public record CandidateAttemptToken(String empresaId, String attemptId) {
    }

    public record CandidateResultToken(String empresaId, String attemptId) {
    }
}
