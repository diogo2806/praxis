package br.com.iforce.praxis.audit;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(UniversalTableAuditingIntegrationTest.AuditTestConfiguration.class)
class UniversalTableAuditingIntegrationTest {

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "created_at",
            "created_by",
            "updated_at",
            "updated_by"
    );

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuditedWriteService auditedWriteService;

    @Test
    void shouldApplyAuditColumnsToEveryApplicationTable() {
        String schema = jdbcTemplate.queryForObject("SELECT current_schema()", String.class);
        List<String> tables = jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_type = 'BASE TABLE'
                  AND table_name <> 'flyway_schema_history'
                ORDER BY table_name
                """, String.class, schema);

        assertThat(tables).isNotEmpty();
        for (String table : tables) {
            List<String> columns = jdbcTemplate.queryForList("""
                    SELECT column_name
                    FROM information_schema.columns
                    WHERE table_schema = ?
                      AND table_name = ?
                    """, String.class, schema, table);
            assertThat(columns)
                    .as("colunas de auditoria da tabela %s", table)
                    .containsAll(REQUIRED_COLUMNS);
        }
    }

    @Test
    void shouldRecordActorAndHistoryForInsertAndUpdate() {
        String code = "AUDIT_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        long planId = auditedWriteService.createPlan(code, "Plano inicial");
        auditedWriteService.renamePlan(planId, "Plano alterado");

        Map<String, Object> plan = jdbcTemplate.queryForMap("""
                SELECT created_by, updated_by, created_at, updated_at
                FROM subscription_plans
                WHERE id = ?
                """, planId);
        assertThat(plan.get("created_by")).isEqualTo("dev-user");
        assertThat(plan.get("updated_by")).isEqualTo("dev-user");
        assertThat(plan.get("created_at")).isNotNull();
        assertThat(plan.get("updated_at")).isNotNull();

        String recordId = "{\"id\": " + planId + "}";
        List<Map<String, Object>> history = jdbcTemplate.queryForList("""
                SELECT operation, actor_user_id, changed_fields, old_data, new_data
                FROM data_change_history
                WHERE table_name = 'subscription_plans'
                  AND record_id = ?
                ORDER BY id
                """, recordId);

        assertThat(history).hasSize(2);
        assertThat(history)
                .extracting(row -> row.get("operation"))
                .containsExactly("INSERT", "UPDATE");
        assertThat(history)
                .extracting(row -> row.get("actor_user_id"))
                .containsOnly("dev-user");
        assertThat(history.get(1).get("changed_fields").toString()).contains("name");
        assertThat(history.get(1).get("old_data").toString()).contains("[REDACTED]");
        assertThat(history.get(1).get("new_data").toString()).contains("[REDACTED]");
    }

    @TestConfiguration
    static class AuditTestConfiguration {

        @Bean
        AuditedWriteService auditedWriteService(EntityManager entityManager) {
            return new AuditedWriteService(entityManager);
        }
    }

    static class AuditedWriteService {

        private final EntityManager entityManager;

        AuditedWriteService(EntityManager entityManager) {
            this.entityManager = entityManager;
        }

        @Transactional
        public long createPlan(String code, String name) {
            Object result = entityManager.createNativeQuery("""
                    INSERT INTO subscription_plans (
                        code,
                        name,
                        plan_type,
                        price_cents,
                        currency,
                        credit_amount,
                        active
                    ) VALUES (
                        :code,
                        :name,
                        'AVULSO',
                        1000,
                        'BRL',
                        1,
                        TRUE
                    )
                    RETURNING id
                    """)
                    .setParameter("code", code)
                    .setParameter("name", name)
                    .getSingleResult();
            return ((Number) result).longValue();
        }

        @Transactional
        public void renamePlan(long planId, String name) {
            entityManager.createNativeQuery("""
                    UPDATE subscription_plans
                    SET name = :name
                    WHERE id = :planId
                    """)
                    .setParameter("name", name)
                    .setParameter("planId", planId)
                    .executeUpdate();
        }
    }
}
