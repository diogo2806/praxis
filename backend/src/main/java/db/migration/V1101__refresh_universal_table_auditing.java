package db.migration;

/**
 * Reexecuta a instalação idempotente da auditoria universal após as migrations
 * que criaram tabelas depois da V1011.
 *
 * <p>A implementação herdada adiciona as quatro colunas de auditoria ausentes
 * e reinstala os gatilhos padronizados em todas as tabelas de negócio.</p>
 */
public class V1101__refresh_universal_table_auditing extends V1011__universal_table_auditing {
}
