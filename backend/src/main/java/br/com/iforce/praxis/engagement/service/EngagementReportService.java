package br.com.iforce.praxis.engagement.service;

import br.com.iforce.praxis.admin.model.EmpresaStatus;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.engagement.dto.EngagementReportSummary;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


/**
 * Gera relatórios periódicos de engajamento para empresas ativas.
 *
 * <p>O relatório conta avaliações concluídas no período. Quando a estimativa de tempo poupado está
 * habilitada, o valor é calculado a partir de uma hipótese configurável e acompanhado da fórmula,
 * fonte metodológica e ressalva de que não se trata de economia observada.</p>
 *
 * <p>Empresas sem conclusões no período não recebem relatório. Empresas sem e-mail corporativo
 * cadastrado são registradas em log e também não recebem a mensagem.</p>
 */
@Service
public class EngagementReportService {

    private static final Logger log = LoggerFactory.getLogger(EngagementReportService.class);
    private static final String DEFAULT_METHODOLOGY_SOURCE =
            "Hipotese operacional configurada pela organizacao";

    private final EmpresaRepository empresaRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final EngagementReportEmailSender emailSender;
    private final int periodDays;
    private final boolean timeSavingEstimateEnabled;
    private final double assumedHoursPerCompletedEvaluation;
    private final String timeSavingMethodologySource;

    public EngagementReportService(
            EmpresaRepository empresaRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            EngagementReportEmailSender emailSender,
            @Value("${praxis.engagement.report-period-days:30}") int periodDays,
            @Value("${praxis.engagement.time-saving-estimate-enabled:true}")
            boolean timeSavingEstimateEnabled,
            @Value("${praxis.engagement.hours-saved-per-evaluation:1.5}")
            double assumedHoursPerCompletedEvaluation,
            @Value("${praxis.engagement.time-saving-methodology-source:Hipotese operacional configurada pela organizacao}")
            String timeSavingMethodologySource
    ) {
        if (periodDays <= 0) {
            throw new IllegalArgumentException("O periodo do relatorio deve ser maior que zero.");
        }
        if (timeSavingEstimateEnabled
                && (!Double.isFinite(assumedHoursPerCompletedEvaluation)
                || assumedHoursPerCompletedEvaluation <= 0)) {
            throw new IllegalArgumentException(
                    "A hipotese de horas por avaliacao deve ser finita e maior que zero quando a estimativa estiver habilitada.");
        }
        this.empresaRepository = empresaRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.emailSender = emailSender;
        this.periodDays = periodDays;
        this.timeSavingEstimateEnabled = timeSavingEstimateEnabled;
        this.assumedHoursPerCompletedEvaluation = assumedHoursPerCompletedEvaluation;
        this.timeSavingMethodologySource = normalizeMethodologySource(timeSavingMethodologySource);
    }

    /**
     * Monta e envia o relatório de engajamento para todos os clientes ativos com uso no período.
     *
     * @param now instante de referência do fim da janela do relatório
     * @return quantidade de relatórios enviados
     */
    @Transactional(readOnly = true)
    public int sendMonthlyReports(Instant now) {
        Instant periodStart = now.minus(periodDays, ChronoUnit.DAYS);
        List<EmpresaEntity> active = empresaRepository.findByStatuses(List.of(EmpresaStatus.ATIVO));
        int sent = 0;
        for (EmpresaEntity empresa : active) {
            long completed = candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtBetween(
                    empresa.getId(), AttemptStatus.COMPLETED, periodStart, now);
            if (completed == 0) {
                continue;
            }
            if (empresa.getCorporateEmail() == null || empresa.getCorporateEmail().isBlank()) {
                log.warn("Cliente {} tem uso no período mas não possui e-mail corporativo; relatório de engajamento não enviado.",
                        empresa.getId());
                continue;
            }

            EngagementReportSummary summary = createSummary(empresa, periodStart, now, completed);
            emailSender.sendMonthlyReport(empresa.getCorporateEmail(), empresa.getName(), summary);
            sent++;
        }
        return sent;
    }

    private EngagementReportSummary createSummary(
            EmpresaEntity empresa,
            Instant periodStart,
            Instant periodEnd,
            long completed
    ) {
        if (!timeSavingEstimateEnabled) {
            return new EngagementReportSummary(
                    empresa.getId(),
                    empresa.getName(),
                    empresa.getCorporateEmail(),
                    periodStart,
                    periodEnd,
                    completed,
                    false,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        double estimatedHoursSaved = completed * assumedHoursPerCompletedEvaluation;
        return new EngagementReportSummary(
                empresa.getId(),
                empresa.getName(),
                empresa.getCorporateEmail(),
                periodStart,
                periodEnd,
                completed,
                true,
                estimatedHoursSaved,
                assumedHoursPerCompletedEvaluation,
                EngagementReportSummary.TIME_SAVING_ESTIMATION_FORMULA,
                timeSavingMethodologySource,
                EngagementReportSummary.TIME_SAVING_ESTIMATION_CAVEAT
        );
    }

    private static String normalizeMethodologySource(String methodologySource) {
        if (methodologySource == null || methodologySource.isBlank()) {
            return DEFAULT_METHODOLOGY_SOURCE;
        }
        return methodologySource.trim();
    }
}
