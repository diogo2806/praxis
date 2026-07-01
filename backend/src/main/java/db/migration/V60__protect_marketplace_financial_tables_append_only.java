package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.sql.Statement;
import java.util.Locale;

public class V60__protect_marketplace_financial_tables_append_only extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        String productName = context.getConnection().getMetaData().getDatabaseProductName();
        if (productName == null || !productName.toLowerCase(Locale.ROOT).contains("postgresql")) {
            return;
        }

        try (Statement statement = context.getConnection().createStatement()) {
            statement.execute("""
                    CREATE OR REPLACE FUNCTION prevent_marketplace_financial_delete()
                    RETURNS trigger AS $$
                    BEGIN
                        RAISE EXCEPTION 'table is append-only';
                    END;
                    $$ LANGUAGE plpgsql
                    """);
            statement.execute("""
                    CREATE OR REPLACE FUNCTION prevent_marketplace_order_amount_mutation()
                    RETURNS trigger AS $$
                    BEGIN
                        IF OLD.listing_id <> NEW.listing_id
                           OR OLD.buyer_tenant_id <> NEW.buyer_tenant_id
                           OR OLD.professional_id <> NEW.professional_id
                           OR OLD.price_cents <> NEW.price_cents
                           OR OLD.platform_fee_cents <> NEW.platform_fee_cents
                           OR OLD.professional_payout_cents <> NEW.professional_payout_cents THEN
                            RAISE EXCEPTION 'marketplace order financial fields are immutable';
                        END IF;
                        RETURN NEW;
                    END;
                    $$ LANGUAGE plpgsql
                    """);
            statement.execute("""
                    CREATE OR REPLACE FUNCTION prevent_marketplace_payout_amount_mutation()
                    RETURNS trigger AS $$
                    BEGIN
                        IF OLD.order_id <> NEW.order_id
                           OR OLD.professional_id <> NEW.professional_id
                           OR OLD.amount_cents <> NEW.amount_cents
                           OR OLD.escrow_release_at <> NEW.escrow_release_at THEN
                            RAISE EXCEPTION 'marketplace payout financial fields are immutable';
                        END IF;
                        RETURN NEW;
                    END;
                    $$ LANGUAGE plpgsql
                    """);
            for (String table : new String[]{"marketplace_orders", "marketplace_payouts"}) {
                statement.execute("DROP TRIGGER IF EXISTS trg_prevent_" + table + "_delete ON " + table);
                statement.execute(
                        "CREATE TRIGGER trg_prevent_" + table + "_delete "
                                + "BEFORE DELETE ON " + table
                                + " FOR EACH ROW EXECUTE FUNCTION prevent_marketplace_financial_delete()");
            }
            statement.execute("DROP TRIGGER IF EXISTS trg_prevent_marketplace_orders_update ON marketplace_orders");
            statement.execute("""
                    CREATE TRIGGER trg_prevent_marketplace_orders_update
                    BEFORE UPDATE ON marketplace_orders
                    FOR EACH ROW EXECUTE FUNCTION prevent_marketplace_order_amount_mutation()
                    """);
            statement.execute("DROP TRIGGER IF EXISTS trg_prevent_marketplace_payouts_update ON marketplace_payouts");
            statement.execute("""
                    CREATE TRIGGER trg_prevent_marketplace_payouts_update
                    BEFORE UPDATE ON marketplace_payouts
                    FOR EACH ROW EXECUTE FUNCTION prevent_marketplace_payout_amount_mutation()
                    """);
        }
    }
}
