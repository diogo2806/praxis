package br.com.iforce.praxis.auth.service;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void preservesCandidateAttemptTokenWhileOriginalWindowIsValid() {
        Instant issuedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

        String token = jwtService.generateCandidateAttemptToken("empresa-1", "att-123", 24, issuedAt);
        Claims claims = jwtService.parse(token);

        assertEquals(issuedAt, claims.getIssuedAt().toInstant());
    }

    @Test
    void renewsCandidateAttemptTokenWhenOriginalWindowExpired() {
        Instant expiredIssuedAt = Instant.now().minus(8, ChronoUnit.DAYS);
        Instant generationStartedAt = Instant.now().minusSeconds(1);

        String token = jwtService.generateCandidateAttemptToken("empresa-1", "att-123", 168, expiredIssuedAt);
        Claims claims = jwtService.parse(token);

        assertTrue(!claims.getIssuedAt().toInstant().isBefore(generationStartedAt));
        assertTrue(claims.getExpiration().toInstant().isAfter(Instant.now()));
        assertEquals("empresa-1", claims.get("empresa_id", String.class));
        assertEquals("att-123", claims.get("attempt_id", String.class));
    }

    @Test
    void resultParserRejectsAttemptToken() {
        String attemptToken = jwtService.generateCandidateAttemptToken("empresa-1", "att-123", 24);

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
