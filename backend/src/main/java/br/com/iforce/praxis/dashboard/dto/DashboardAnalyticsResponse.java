package br.com.iforce.praxis.dashboard.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Indicadores analíticos usados exclusivamente pelo dashboard da empresa.
 *
 * @param generatedAt instante em que os indicadores foram calculados
 * @param periodDays quantidade de dias exibida no gráfico de atividade
 * @param participations resumo consolidado das execuções de avaliações
 * @param activity série diária de participações criadas, concluídas e abandonadas
 */
public record DashboardAnalyticsResponse(
        Instant generatedAt,
        int periodDays,
        ParticipationSummary participations,
        List<ActivityPoint> activity
) {

    /**
     * Resumo do ciclo de execução das avaliações da empresa.
     *
     * @param total total de participações criadas
     * @param started participações que chegaram a ser iniciadas
     * @param notStarted participações ainda não iniciadas
     * @param inProgress participações em andamento
     * @param completed participações concluídas
     * @param abandoned participações abandonadas
     * @param expired participações expiradas
     * @param completionRatePercent percentual de conclusão entre participações encerradas
     * @param dropOffRatePercent percentual de abandono ou expiração entre participações encerradas
     * @param averageScoreLast30Days média das notas concluídas no período, quando houver
     */
    public record ParticipationSummary(
            long total,
            long started,
            long notStarted,
            long inProgress,
            long completed,
            long abandoned,
            long expired,
            double completionRatePercent,
            double dropOffRatePercent,
            Double averageScoreLast30Days
    ) {
    }

    /**
     * Um ponto diário do gráfico de atividade.
     *
     * @param date data de referência em UTC
     * @param created participações criadas na data
     * @param completed participações concluídas na data
     * @param abandoned participações abandonadas ou expiradas na data
     */
    public record ActivityPoint(
            LocalDate date,
            long created,
            long completed,
            long abandoned
    ) {
    }
}
