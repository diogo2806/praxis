package br.com.iforce.praxis.audit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Garante que a transação seja aberta antes de o aspecto registrar o ator no
 * contexto local da conexão PostgreSQL.
 */
@Configuration
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
public class DatabaseAuditConfiguration {
}
