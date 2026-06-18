package br.com.iforce.praxis.shared.jpa;

import br.com.iforce.praxis.auth.context.TenantContextHolder;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

import java.io.Serializable;

public class TenantHibernateInterceptor extends EmptyInterceptor {

    @Override
    public boolean onLoad(
            Object entity,
            Serializable id,
            Object[] state,
            String[] propertyNames,
            Type[] types
    ) {
        if (entity instanceof TenantAwareEntity tenantAware) {
            String tenantId = TenantContextHolder.get();
            if (tenantId != null && !tenantAware.getTenantId().equals(tenantId)) {
                throw new SecurityException("Acesso negado: entidade pertence a outro tenant");
            }
        }
        return false;
    }

    @Override
    public boolean onSave(
            Object entity,
            Serializable id,
            Object[] state,
            String[] propertyNames,
            Type[] types
    ) {
        if (entity instanceof TenantAwareEntity tenantAware) {
            if (tenantAware.getTenantId() == null || tenantAware.getTenantId().isBlank()) {
                throw new IllegalStateException("Entidade tenant-aware deve possuir tenantId.");
            }
            String tenantId = TenantContextHolder.get();
            if (tenantId != null && !tenantAware.getTenantId().equals(tenantId)) {
                throw new SecurityException("Acesso negado: entidade pertence a outro tenant");
            }
        }
        return false;
    }
}
