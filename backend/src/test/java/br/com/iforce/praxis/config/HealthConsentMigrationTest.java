package br.com.iforce.praxis.config;

import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class HealthConsentMigrationTest {

    @Test
    void completionMigrationRunsAfterInitialConsentMigration() throws IOException {
        assertThat(MigrationVersion.fromVersion("1019"))
                .isLessThan(MigrationVersion.fromVersion("1020"));

        String migration = readResource("/db/migration/V1020__complete_health_consent_state.sql");
        assertThat(migration)
                .contains("health_consent_source")
                .contains("ck_candidate_attempt_health_consent_complete")
                .contains("ck_candidate_attempt_health_consent_revoked");
    }

    private String readResource(String path) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(path);
        assertThat(inputStream).as("Recurso de migration %s", path).isNotNull();
        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
