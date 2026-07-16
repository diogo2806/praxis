package br.com.iforce.praxis.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class FlywayMigrationCompatibilityTest {

    private static final Pattern VERSIONED_MIGRATION = Pattern.compile(
            "^V([0-9]+(?:_[0-9]+)*)__.+\\.sql$"
    );

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
                "/db/migration/V1008__drop_obsolete_gupy_callback_confirmation_index.sql"
        );

        assertThat(migration)
                .contains("DROP INDEX IF EXISTS uq_outbox_gupy_callback_confirmation");
    }

    @Test
    void outOfOrderMigrationsRemainEnabledForProvisionedDatabases() throws IOException {
        String properties = readResource("/application.properties");

        assertThat(properties)
                .contains("spring.flyway.out-of-order=${SPRING_FLYWAY_OUT_OF_ORDER:true}");
    }

    @Test
    void testProfileUsesOnlyTheCanonicalFlywayLocation() throws IOException {
        String properties = readResource("/application.properties");

        assertThat(properties)
                .contains("spring.flyway.locations=classpath:db/migration")
                .doesNotContain("classpath:db/migration/{vendor}");
    }

    @Test
    void versionedMigrationsUseUniqueVersions() throws IOException {
        Path migrationRoot = Path.of("src/main/resources/db/migration");
        Map<String, List<Path>> migrationsByVersion = new TreeMap<>();

        try (Stream<Path> migrations = Files.walk(migrationRoot)) {
            List<Path> migrationFiles = migrations
                    .filter(Files::isRegularFile)
                    .toList();

            for (Path migrationFile : migrationFiles) {
                Matcher matcher = VERSIONED_MIGRATION.matcher(migrationFile.getFileName().toString());
                if (!matcher.matches()) {
                    continue;
                }

                String version = matcher.group(1).replace('_', '.');
                migrationsByVersion
                        .computeIfAbsent(version, ignored -> new ArrayList<>())
                        .add(migrationFile);
            }
        }

        List<Map.Entry<String, List<Path>>> duplicates = migrationsByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .toList();

        assertThat(duplicates)
                .as("Versões Flyway duplicadas em %s", migrationRoot)
                .isEmpty();
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
