package br.com.iforce.praxis.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceCandidateResultTokenTest {

    private static final String SECRET = "test-secret-with-at-least-thirty-two-characters";

    private final JwtService jwtService = new JwtService(SECRET, 8, false);

    @Test
    void generatesAndParsesCandidateResultToken() {
        String token = jwtService.generateCandidateResultToken("empresa-1", "att-123", 720);

        JwtService.CandidateResultToken parsed = jwtService.parseCandidateResultToken(token);

        assertEquals("empresa-1", parsed.empresaId());
        assertEquals("att-123", parsed.attemptId());
    }

    @Test
    void preservesCandidateAttemptTokenWhileWindowIsValid() {
        Instant issuedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

        String first = jwtService.generateCandidateAttemptToken("empresa-1", "att-123", 24, issuedAt);
        String second = jwtService.generateCandidateAttemptToken("empresa-1", "att-123", 24, issuedAt);
        Claims claims = jwtService.parse(first);

        assertEquals(first, second);
        assertEquals(issuedAt, claims.getIssuedAt().toInstant());
    }

    @Test
    void usesExactPersistedExpiration() {
        Instant issuedAt = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        String token = jwtService.generateCandidateAttemptToken(
                "empresa-1",
                "att-123",
                issuedAt,
                expiresAt
        );
        Claims claims = jwtService.parse(token);

        assertEquals(issuedAt, claims.getIssuedAt().toInstant());
        assertEquals(expiresAt, claims.getExpiration().toInstant());
        assertEquals("empresa-1", claims.get("empresa_id", String.class));
        assertEquals("att-123", claims.get("attempt_id", String.class));
    }

    @Test
    void doesNotRenewExpiredWindowImplicitly() {
        Instant issuedAt = Instant.now().minus(8, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant expiresAt = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);

        String token = jwtService.generateCandidateAttemptToken(
                "empresa-1",
                "att-123",
                issuedAt,
                expiresAt
        );

        assertThrows(ExpiredJwtException.class, () -> jwtService.parse(token));
    }

    @Test
    void resultParserRejectsAttemptToken() {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String attemptToken = jwtService.generateCandidateAttemptToken(
                "empresa-1",
                "att-123",
                24,
                issuedAt
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.parseCandidateResultToken(attemptToken)
        );
    }

    @Test
    void attemptParserRejectsResultToken() {
        String resultToken = jwtService.generateCandidateResultToken("empresa-1", "att-123", 720);

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtService.parseCandidateAttemptToken(resultToken)
        );
    }
}
