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

/**
 * Cria e valida tokens de segurança (JWT).
 *
 * JWT (JSON Web Token) é um padrão de token assinado digitalmente que prova
 * a identidade de um usuário. O token contém:
 * - Quem é o usuário (ID)
 * - Qual empresa ele pertence (tenant)
 * - Quais permissões tem (roles)
 * - Quando expira (TTL)
 *
 * Dois tipos de tokens são gerados:
 * 1. Para administradores/recrutadores: dura 8 horas (configurável)
 * 2. Para candidatos em prova: dura apenas o tempo que levam para fazer a prova
 */
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

    /**
     * Cria um token de sessão para um usuário (administrador/recrutador).
     *
     * O token contém a identidade do usuário, sua empresa, e suas permissões.
     * É válido por um período configurado (padrão: 8 horas). Após expirar,
     * o usuário precisa fazer login novamente.
     *
     * @param userId ID único do usuário
     * @param tenantId ID da empresa do usuário
     * @param roles Conjunto de permissões (ex: ADMIN, RECRUITER)
     * @return Token JWT assinado que pode ser enviado em requisições
     */
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

    /**
     * Cria um token para um candidato fazendo uma prova.
     *
     * Este token é mais restritivo que o de administrador: vale apenas enquanto
     * o candidato está fazendo a prova. Contém o ID da tentativa e da empresa.
     *
     * O tempo de validade é passado como parâmetro para que seja flexível:
     * uma prova de 1 hora recebe um token de 1 hora, uma de 2 horas recebe 2 horas.
     *
     * @param tenantId ID da empresa
     * @param attemptId ID da tentativa da prova
     * @param ttlHours Tempo de validade do token em horas
     * @return Token JWT para usar durante a prova
     */
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

    /**
     * Extrai as informações de um token de prova.
     *
     * Valida a assinatura do token (garante que não foi falsificado) e extrai
     * o ID da tentativa e da empresa.
     *
     * @param token Token JWT recebido do candidato
     * @return ID da tentativa e da empresa contidos no token
     * @throws IllegalArgumentException se o token é inválido ou expirou
     */
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

    /**
     * Decodifica um token JWT.
     *
     * Valida a assinatura e retorna todas as informações (claims) contidas no token.
     * Este é o método de baixo nível; use os métodos especializados acima em vez deste.
     *
     * @param token Token JWT assinado
     * @return Dados decodificados do token
     * @throws RuntimeException se o token é inválido ou expirou
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Dados extraídos do link do candidato: a empresa e a participação a que ele dá acesso. */
    public record CandidateAttemptToken(String tenantId, String attemptId) {
    }
}
