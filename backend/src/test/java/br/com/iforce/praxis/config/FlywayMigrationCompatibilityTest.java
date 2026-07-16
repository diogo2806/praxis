package br.com.iforce.praxis.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationCompatibilityTest {

    @Test
    void callbackMigrationRemainsCompatibleWithLegacyV1001Execution() throws IOException {
        String migration = readResource(
                "/db/migration/V1004__gupy_callback_confirmation_outbox.sql"
        );

        assertThat(migration)
                .contains("CREATE UNIQUE INDEX IF NOT EXISTS uq_outbox_gupy_callback_confirmation");
    }

    @Test
    void obsoleteCallbackIndexIsRemovedAfterCallbackDeliveryIsDisabled() throws IOException {
        String migration = readResource(
                "/db/migration/V1006__drop_obsolete_gupy_callback_confirmation_index.sql"
        );

        assertThat(migration)
                .contains("DROP INDEX IF EXISTS uq_outbox_gupy_callback_confirmation");
    }

    private String readResource(String path) throws IOException {
        InputStream inputStream = getClass().getResourceAsStream(path);
        assertThat(inputStream)
                .as("Recurso de migration %s", path)
                .isNotNull();

        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
