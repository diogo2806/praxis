package br.com.iforce.praxis.admin.model;

/**
 * Situação operacional do cliente (tenant) controlada pelo painel administrativo.
 *
 * <ul>
 *     <li>{@link #ATIVO}: cliente com acesso liberado.</li>
 *     <li>{@link #EM_TESTE}: cliente em período de teste.</li>
 *     <li>{@link #SUSPENSO}: cliente bloqueado; não autentica nem consome APIs protegidas.</li>
 *     <li>{@link #CANCELADO}: cliente sem acesso ativo; histórico é preservado.</li>
 * </ul>
 *
 * <p>Os status financeiros ({@code PENDENTE_PAGAMENTO}, {@code INADIMPLENTE},
 * {@code SEM_CREDITO}) entram apenas na Parte B (cobrança Mercado Pago).</p>
 */
public enum TenantStatus {
    ATIVO,
    EM_TESTE,
    SUSPENSO,
    CANCELADO;

    /** Indica se o status impede autenticação e consumo de APIs protegidas. */
    public boolean blocksAccess() {
        return this == SUSPENSO || this == CANCELADO;
    }
}
