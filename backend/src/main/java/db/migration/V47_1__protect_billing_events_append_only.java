package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;

import org.flywaydb.core.api.migration.Context;


import java.sql.Statement;

import java.util.Locale;


/**
 * Reforça, no banco, a natureza append-only dos eventos financeiros e do ledger de créditos.
 *
 * <p>Espelha a proteção já aplicada a {@code audit_events}: triggers impedem UPDATE/DELETE.
 * Só roda no PostgreSQL; em H2 (testes) é ignorada — a regra também é garantida na aplicação,
 * que nunca expõe operações de edição/exclusão dessas tabelas.</p>
 */
public class V47_1__protect_billing_events_append_only extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        String productName = context.getConnection().getMetaData().getDatabaseProductName();
        if (productName == null || !productName.toLowerCase(Locale.ROOT).contains("postgresql")) {
            return;
        }

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    CREATE OR REPLACE FUNCTION prevent_billing_mutation()
                    RETURNS trigger AS $$
                    BEGIN
                        RAISE EXCEPTION 'table is append-only';
                    END;
                    $$ LANGUAGE plpgsql
                    """);
            for (String table : new String[]{"empresa_billing_events", "empresa_credit_ledger"}) {
                statement.execute("DROP TRIGGER IF EXISTS trg_prevent_" + table + "_update ON " + table);
                statement.execute(
                        "CREATE TRIGGER trg_prevent_" + table + "_update "
                                + "BEFORE UPDATE ON " + table
                                + " FOR EACH ROW EXECUTE FUNCTION prevent_billing_mutation()");
                statement.execute("DROP TRIGGER IF EXISTS trg_prevent_" + table + "_delete ON " + table);
                statement.execute(
                        "CREATE TRIGGER trg_prevent_" + table + "_delete "
                                + "BEFORE DELETE ON " + table
                                + " FOR EACH ROW EXECUTE FUNCTION prevent_billing_mutation()");
            }
        }
    }
}
