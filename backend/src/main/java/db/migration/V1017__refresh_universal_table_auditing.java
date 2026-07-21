package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Reaplica a infraestrutura de auditoria universal após as migrations que
 * criaram tabelas depois da versão 1011. Precisa ser a última migration para
 * cobrir também as tabelas alteradas na versão 1016 (aceite de termos).
 */
public class V1017__refresh_universal_table_auditing extends BaseJavaMigration {

    @Override
    public void migrate(Context context) throws Exception {
        new V1011__universal_table_auditing().migrate(context);
    }
}
