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
     * O instante inicial da credencial vem da criação persistida da tentativa.
     * Enquanto a credencial estiver válida, repetições idempotentes geram o mesmo JWT.
     * Quando ela já expirou, uma nova geração de link emite outra janela de validade,
     * evitando que as ações autenticadas de copiar ou reenviar devolvam um link morto.
     */
    public String generateCandidateAttemptToken(String empresaId, String attemptId, int ttlHours) {
        return generateCandidateAttemptToken(
                empresaId,
                attemptId,
                ttlHours,
                candidateAttemptIssuedAt(attemptId)
        );
    }

    /**
     * Gera o token usando o instante persistido já carregado pelo chamador.
     * Evita uma nova consulta por tentativa em listagens paginadas.
     */
    public String generateCandidateAttemptToken(
            String empresaId,
            String attemptId,
            int ttlHours,
            Instant issuedAt
    ) {
        if (issuedAt == null) {
            throw new IllegalArgumentException("O instante de criação da tentativa é obrigatório.");
        }
        return generateCandidateScopedToken(
                empresaId,
                attemptId,
                CANDIDATE_ATTEMPT_TOKEN_TYPE,
                ttlHours,
                renewedIssuedAtWhenExpired(issuedAt, ttlHours)
        );
    }

    public String generateCandidateResultToken(String empresaId, String attemptId, int ttlHours) {
        return generateCandidateScopedToken(
                empresaId,
                attemptId,
                CANDIDATE_RESULT_TOKEN_TYPE,
                ttlHours,
                Instant.now()
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

    private Instant candidateAttemptIssuedAt(String attemptId) {
        if (candidateAttemptRepositoryProvider == null) {
            return Instant.now();
        }
        CandidateAttemptRepository repository = candidateAttemptRepositoryProvider.getIfAvailable();
        if (repository == null) {
            return Instant.now();
        }
        return repository.findById(attemptId)
                .map(CandidateAttemptEntity::getCreatedAt)
                .filter(createdAt -> createdAt != null)
                .orElseGet(Instant::now);
    }

    private Instant renewedIssuedAtWhenExpired(Instant issuedAt, int ttlHours) {
        if (ttlHours <= 0) {
            throw new IllegalArgumentException("A validade do token deve ser maior que zero.");
        }
        Instant now = Instant.now();
        Instant expiration = issuedAt.plusSeconds(ttlHours * 60L * 60L);
        return expiration.isAfter(now) ? issuedAt : now;
    }

    private String generateCandidateScopedToken(
            String empresaId,
            String attemptId,
            String type,
            int ttlHours,
            Instant issuedAt
    ) {
        if (ttlHours <= 0) {
            throw new IllegalArgumentException("A validade do token deve ser maior que zero.");
        }
        return Jwts.builder()
                .subject(attemptId)
                .claim("typ", type)
                .claim("empresa_id", empresaId)
                .claim("attempt_id", attemptId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plusSeconds(ttlHours * 60L * 60L)))
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
