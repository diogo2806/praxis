package br.com.iforce.praxis.auth.context;

public final class TenantContextHolder {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(String tenantId) {
        TENANT.set(tenantId);
    }

    public static String get() {
        return TENANT.get();
    }

    public static String getRequired() {
        String tenant = TENANT.get();
        if (tenant == null) {
            throw new IllegalStateException("Empresa obrigatória não foi estabelecida no contexto");
        }
        return tenant;
    }

    public static void clear() {
        TENANT.remove();
    }
}
