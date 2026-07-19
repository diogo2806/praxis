package br.com.iforce.praxis.audit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Impede que a aplicação suba caso uma migration futura crie uma tabela sem os
 * campos obrigatórios de auditoria ou, no PostgreSQL, sem os gatilhos universais.
 */
@Component
public class UniversalAuditSchemaValidator implements ApplicationRunner {

    private static final Set<String> REQUIRED_COLUMNS = Set.of(
            "created_at",
            "created_by",
            "updated_at",
            "updated_by"
    );
    private static final String HISTORY_TABLE = "data_change_history";
    private static final String FLYWAY_TABLE = "flyway_schema_history";

    private final DataSource dataSource;
    private final boolean validationEnabled;

    public UniversalAuditSchemaValidator(
            DataSource dataSource,
            @Value("${praxis.audit.schema-validation-enabled:true}") boolean validationEnabled
    ) {
        this.dataSource = dataSource;
        this.validationEnabled = validationEnabled;
    }

    @Override
    public void run(ApplicationArguments arguments) throws Exception {
        if (!validationEnabled) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            String schema = resolveCurrentSchema(connection);
            boolean postgreSql = isPostgreSql(connection);
            List<String> violations = new ArrayList<>();

            for (String table : loadApplicationTables(connection, schema)) {
                Set<String> columns = loadColumns(connection, schema, table);
                for (String requiredColumn : REQUIRED_COLUMNS) {
                    if (!columns.contains(requiredColumn)) {
                        violations.add(table + ": coluna ausente " + requiredColumn);
                    }
                }

                if (postgreSql && !HISTORY_TABLE.equalsIgnoreCase(table)) {
                    Set<String> triggers = loadPostgreSqlTriggers(connection, schema, table);
                    if (!triggers.contains("trg_praxis_audit_columns")) {
                        violations.add(table + ": gatilho ausente trg_praxis_audit_columns");
                    }
                    if (!triggers.contains("trg_praxis_row_history")) {
                        violations.add(table + ": gatilho ausente trg_praxis_row_history");
                    }
                }
            }

            if (!violations.isEmpty()) {
                throw new IllegalStateException(
                        "Cobertura de auditoria universal incompleta: " + String.join("; ", violations)
                );
            }
        }
    }

    private List<String> loadApplicationTables(Connection connection, String schema) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getTables(
                connection.getCatalog(),
                schema,
                "%",
                new String[]{"TABLE"}
        )) {
            while (resultSet.next()) {
                String tableSchema = resultSet.getString("TABLE_SCHEM");
                String tableName = resultSet.getString("TABLE_NAME");
                if (tableSchema == null || tableName == null || !tableSchema.equalsIgnoreCase(schema)) {
                    continue;
                }
                if (FLYWAY_TABLE.equalsIgnoreCase(tableName)) {
                    continue;
                }
                tables.add(tableName);
            }
        }
        return tables;
    }

    private Set<String> loadColumns(
            Connection connection,
            String schema,
            String table
    ) throws SQLException {
        Set<String> columns = new HashSet<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getColumns(
                connection.getCatalog(),
                schema,
                table,
                "%"
        )) {
            while (resultSet.next()) {
                String columnName = resultSet.getString("COLUMN_NAME");
                if (columnName != null) {
                    columns.add(columnName.toLowerCase(Locale.ROOT));
                }
            }
        }
        return columns;
    }

    private Set<String> loadPostgreSqlTriggers(
            Connection connection,
            String schema,
            String table
    ) throws SQLException {
        Set<String> triggers = new HashSet<>();
        String sql = """
                SELECT DISTINCT trigger_name
                FROM information_schema.triggers
                WHERE event_object_schema = ?
                  AND event_object_table = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    triggers.add(resultSet.getString(1));
                }
            }
        }
        return triggers;
    }

    private String resolveCurrentSchema(Connection connection) throws SQLException {
        String schema = connection.getSchema();
        if (schema != null && !schema.isBlank()) {
            return schema;
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT current_schema()")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString(1);
                }
            }
        }
        throw new SQLException("Não foi possível identificar o schema atual para validar a auditoria.");
    }

    private boolean isPostgreSql(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName();
        return productName != null && productName.toLowerCase(Locale.ROOT).contains("postgresql");
    }
}
