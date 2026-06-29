package br.com.iforce.praxis.admin.model;

/**
 * Rótulo comercial do cliente (tenant) no painel administrativo.
 *
 * <p>Na Parte A do painel ADMIN este enum é apenas um rótulo comercial usado para
 * classificar o cliente. Na Parte B (cobrança Mercado Pago) cada plano ganha um
 * comportamento financeiro próprio:</p>
 *
 * <ul>
 *     <li>{@link #AVULSO}: crédito pré-pago, o cliente compra pacotes de avaliações.</li>
 *     <li>{@link #PROFISSIONAL}: assinatura recorrente mensal.</li>
 *     <li>{@link #ENTERPRISE}: contrato personalizado/manual.</li>
 * </ul>
 */
public enum CommercialPlanType {
    AVULSO,
    PROFISSIONAL,
    ENTERPRISE
}
