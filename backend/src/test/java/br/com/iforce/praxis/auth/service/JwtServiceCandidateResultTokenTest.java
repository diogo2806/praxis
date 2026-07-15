package br.com.iforce.praxis.auth.service;

import org.junit.jupiter.api.Test;

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
