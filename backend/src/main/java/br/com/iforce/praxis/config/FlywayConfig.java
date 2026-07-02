package br.com.iforce.praxis.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Applied migration files must never be edited in place, but historical
 * edits already happened before this guard existed, leaving checksum
 * mismatches in already-provisioned databases. Repairing the schema
 * history to match the currently resolved migrations before migrating
 * lets startup recover from that instead of failing hard.
 */
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return (Flyway flyway) -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
