package br.com.iforce.praxis.admin.model;

/**
 * Faixa de saúde de retenção de um cliente ativo, derivada da comparação entre o volume de
 * avaliações concluídas nos últimos 30 dias e no período anterior de mesmo tamanho.
 *
 * <ul>
 *     <li>{@link #HEALTHY}: uso estável ou em crescimento — não há sinal de evasão.</li>
 *     <li>{@link #AT_RISK}: uso caiu além do limite configurado (por padrão, mais de 30%);
 *         é a fila de atuação para a equipe de Customer Success intervir proativamente.</li>
 *     <li>{@link #NO_BASELINE}: o cliente não tem histórico suficiente no período anterior para
 *         que a queda seja estatisticamente relevante; nenhum alerta é disparado.</li>
 * </ul>
 */
public enum CustomerHealthLevel {
    HEALTHY,
    AT_RISK,
    NO_BASELINE
}
