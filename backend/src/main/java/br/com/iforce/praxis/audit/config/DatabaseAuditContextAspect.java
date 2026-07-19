package br.com.iforce.praxis.audit.config;

import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;

/**
 * Publica o usuário autenticado e o identificador da requisição na conexão da
 * transação. Os gatilhos de banco utilizam essas informações para preencher os
 * campos created_by/updated_by e a trilha de alterações.
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class DatabaseAuditContextAspect {

    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final String ANONYMOUS_ACTOR = "ANONYMOUS";
    private static final String ANONYMOUS_PRINCIPAL = "anonymousUser";
    private static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final int MAX_VALUE_LENGTH = 120;

    private final EntityManager entityManager;
    private final boolean securityEnabled;
    private final String defaultUserId;
    private volatile Boolean postgreSql;

    public DatabaseAuditContextAspect(
            EntityManager entityManager,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.default-user-id:dev-user}") String defaultUserId
    ) {
        this.entityManager = entityManager;
        this.securityEnabled = securityEnabled;
        this.defaultUserId = defaultUserId;
    }

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)"
            + " || @within(org.springframework.transaction.annotation.Transactional)")
    public Object bindAuditContext(ProceedingJoinPoint joinPoint) throws Throwable {
        if (TransactionSynchronizationManager.isActualTransactionActive() && isPostgreSql()) {
            setLocalSetting("praxis.actor_user_id", resolveActor());
            setLocalSetting("praxis.request_id", resolveRequestId());
        }
        return joinPoint.proceed();
    }

    private boolean isPostgreSql() {
        Boolean cached = postgreSql;
        if (cached != null) {
            return cached;
        }

        Session session = entityManager.unwrap(Session.class);
        String productName = session.doReturningWork(
                connection -> connection.getMetaData().getDatabaseProductName()
        );
        boolean detected = productName != null
                && productName.toLowerCase(Locale.ROOT).contains("postgresql");
        postgreSql = detected;
        return detected;
    }

    private void setLocalSetting(String name, String value) {
        entityManager.createNativeQuery("SELECT set_config(:name, :value, true)")
                .setParameter("name", name)
                .setParameter("value", value)
                .getSingleResult();
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !ANONYMOUS_PRINCIPAL.equals(authentication.getPrincipal())) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof String userId && !userId.isBlank()) {
                return sanitize(userId, SYSTEM_ACTOR);
            }
            return sanitize(authentication.getName(), SYSTEM_ACTOR);
        }

        if (!securityEnabled) {
            return sanitize(defaultUserId, SYSTEM_ACTOR);
        }

        return MDC.get(REQUEST_ID_MDC_KEY) == null ? SYSTEM_ACTOR : ANONYMOUS_ACTOR;
    }

    private String resolveRequestId() {
        return sanitize(MDC.get(REQUEST_ID_MDC_KEY), "");
    }

    private String sanitize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String sanitized = value.replace('\r', '_').replace('\n', '_').trim();
        return sanitized.length() <= MAX_VALUE_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_VALUE_LENGTH);
    }
}
