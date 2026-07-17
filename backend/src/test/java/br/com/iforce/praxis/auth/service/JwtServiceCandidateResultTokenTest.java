package br.com.iforce.praxis.auth.service;

import io.jsonwebtoken.Claims;
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
    void preservesCanonicalCandidateAttemptIssuedAt() {
        Instant issuedAt = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

        String token = jwtService.generateCandidateAttemptToken("empresa-1", "att-123", 24, issuedAt);
        Claims claims = jwtService.parse(token);

        assertEquals(issuedAt, claims.getIssuedAt().toInstant());
    }

    @Test
    void generatesSameCandidateAttemptTokenForSameCanonicalWindow() {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        String first = jwtService.generateCandidateAttemptToken("empresa-1", "att-123", 24, issuedAt);
        String second = jwtService.generateCandidateAttemptToken("empresa-1", "att-123", 24, issuedAt);

        assertEquals(first, second);
    }

    @Test
    void failsExplicitlyWhenCanonicalWindowServiceIsUnavailable() {
        assertThrows(
                IllegalStateException.class,
                () -> jwtService.generateCandidateAttemptToken("empresa-1", "att-123", 24)
        );
    }

    @Test
    void resultParserRejectsAttemptToken() {
        Instant issuedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        String attemptToken = jwtService.generateCandidateAttemptToken("empresa-1", "att-123", 24, issuedAt);

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
