package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;

import org.flywaydb.core.api.migration.Context;


import java.sql.Statement;

import java.util.Locale;


public class V13_1__protect_audit_events_append_only extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        String productName = context.getConnection().getMetaData().getDatabaseProductName();
        if (productName == null || !productName.toLowerCase(Locale.ROOT).contains("postgresql")) {
            return;
        }

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    CREATE OR REPLACE FUNCTION prevent_audit_events_mutation()
                    RETURNS trigger AS $$
                    BEGIN
                        RAISE EXCEPTION 'audit_events is append-only';
                    END;
                    $$ LANGUAGE plpgsql
                    """);
            statement.execute("DROP TRIGGER IF EXISTS trg_prevent_audit_events_update ON audit_events");
            statement.execute("""
                    CREATE TRIGGER trg_prevent_audit_events_update
                    BEFORE UPDATE ON audit_events
                    FOR EACH ROW
                    EXECUTE FUNCTION prevent_audit_events_mutation()
                    """);
            statement.execute("DROP TRIGGER IF EXISTS trg_prevent_audit_events_delete ON audit_events");
            statement.execute("""
                    CREATE TRIGGER trg_prevent_audit_events_delete
                    BEFORE DELETE ON audit_events
                    FOR EACH ROW
                    EXECUTE FUNCTION prevent_audit_events_mutation()
                    """);
        }
    }
}
