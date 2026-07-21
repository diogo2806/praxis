package br.com.iforce.praxis.config;

import org.flywaydb.core.api.MigrationVersion;
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
            "^V([0-9]+(?:_[0-9]+)*)__.+\\.(?:sql|java)$"
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
    void candidateTokenMigrationsPreserveHistoricalVersionsAndDependencyOrder() throws IOException {
        MigrationVersion issuedAtVersion = MigrationVersion.fromVersion("1009");
        MigrationVersion partnerVersion = MigrationVersion.fromVersion("1009.1");
        MigrationVersion expiresAtVersion = MigrationVersion.fromVersion("1010");
        MigrationVersion auditingVersion = MigrationVersion.fromVersion("1011");
        MigrationVersion annualPlansVersion = MigrationVersion.fromVersion("1012");
        MigrationVersion integrityTelemetryVersion = MigrationVersion.fromVersion("1013");
        MigrationVersion complianceVersion = MigrationVersion.fromVersion("1014");
        MigrationVersion journeyLifecycleVersion = MigrationVersion.fromVersion("1015");
        MigrationVersion termsAcceptanceVersion = MigrationVersion.fromVersion("1016");
        MigrationVersion auditRefreshVersion = MigrationVersion.fromVersion("1017");

        assertThat(issuedAtVersion).isLessThan(partnerVersion);
        assertThat(partnerVersion).isLessThan(expiresAtVersion);
        assertThat(expiresAtVersion).isLessThan(auditingVersion);
        assertThat(auditingVersion).isLessThan(annualPlansVersion);
        assertThat(annualPlansVersion).isLessThan(integrityTelemetryVersion);
        assertThat(integrityTelemetryVersion).isLessThan(complianceVersion);
        assertThat(complianceVersion).isLessThan(journeyLifecycleVersion);
        assertThat(journeyLifecycleVersion).isLessThan(termsAcceptanceVersion);
        // A reaplicação da auditoria universal precisa ser a última migration, para
        // cobrir também as tabelas alteradas pelo aceite de termos (versão 1016).
        assertThat(termsAcceptanceVersion).isLessThan(auditRefreshVersion);

        assertThat(readResource("/db/migration/V1009__persist_candidate_token_window.sql"))
                .contains("ADD COLUMN candidate_token_issued_at");
        assertThat(readResource("/db/migration/V1009_1__create_partner_distribution_module.sql"))
                .contains("CREATE TABLE partner_clients");
        assertThat(readResource("/db/migration/V1010__add_candidate_token_expiration.sql"))
                .contains("candidate_token_issued_at + INTERVAL '168 hours'");
        assertThat(readResource("/db/migration/V1012__replace_professional_plans_with_annual_pools.sql"))
                .contains("'PRO_ANNUAL_100'");
    }

    @Test
    void versionedMigrationsUseUniqueVersions() throws IOException {
        // As migrations SQL e as migrations Java compartilham o mesmo espaço de versões
        // no Flyway, então a checagem de duplicidade precisa cobrir as duas origens.
        List<Path> migrationRoots = List.of(
                Path.of("src/main/resources/db/migration"),
                Path.of("src/main/java/db/migration")
        );
        Map<String, List<Path>> migrationsByVersion = new TreeMap<>();

        for (Path migrationRoot : migrationRoots) {
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
        }

        List<Map.Entry<String, List<Path>>> duplicates = migrationsByVersion.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .toList();

        assertThat(duplicates)
                .as("Versões Flyway duplicadas em %s", migrationRoots)
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
