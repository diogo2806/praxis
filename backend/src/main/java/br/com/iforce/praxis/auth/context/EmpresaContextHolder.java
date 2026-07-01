package br.com.iforce.praxis.auth.context;

public final class EmpresaContextHolder {

    private static final ThreadLocal<String> EMPRESA = new ThreadLocal<>();

    private EmpresaContextHolder() {
    }

    public static void set(String empresaId) {
        EMPRESA.set(empresaId);
    }

    public static String get() {
        return EMPRESA.get();
    }

    public static String getRequired() {
        String empresa = EMPRESA.get();
        if (empresa == null) {
            throw new IllegalStateException("Empresa obrigatória não foi estabelecida no contexto");
        }
        return empresa;
    }

    public static void clear() {
        EMPRESA.remove();
    }
}
