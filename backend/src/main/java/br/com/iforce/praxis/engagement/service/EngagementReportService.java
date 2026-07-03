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
 * Relatórios de engajamento automatizados — o worker que reforça o valor contínuo da Práxis.
 *
 * <p>Na visão do processo, uma vez por mês o sistema soma quantas avaliações cada cliente ativo
 * concluiu no período e traduz esse volume em "horas economizadas com avaliações Práxis". A
 * mensagem vai para o e-mail corporativo do cliente ({@code corporateEmail}), lembrando os
 * administradores (papel {@code EMPRESA}) do retorno que a plataforma vem entregando — um empurrão
 * de retenção antes que o valor percebido esfrie.</p>
 *
 * <p>Clientes sem nenhuma conclusão no período não recebem relatório (não há valor a reforçar), e
 * clientes sem e-mail corporativo cadastrado são apenas registrados em log.</p>
 */
@Service
public class EngagementReportService {

    private static final Logger log = LoggerFactory.getLogger(EngagementReportService.class);

    private final EmpresaRepository empresaRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final EngagementReportEmailSender emailSender;
    private final int periodDays;
    private final double hoursSavedPerEvaluation;

    public EngagementReportService(
            EmpresaRepository empresaRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            EngagementReportEmailSender emailSender,
            @Value("${praxis.engagement.report-period-days:30}") int periodDays,
            @Value("${praxis.engagement.hours-saved-per-evaluation:1.5}") double hoursSavedPerEvaluation
    ) {
        this.empresaRepository = empresaRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.emailSender = emailSender;
        this.periodDays = periodDays;
        this.hoursSavedPerEvaluation = hoursSavedPerEvaluation;
    }

    /**
     * Monta e envia o relatório mensal de engajamento para todos os clientes ativos com uso no período.
     *
     * <p>Fluxo do processo: para cada cliente com acesso liberado ({@link EmpresaStatus#ATIVO}),
     * conta as avaliações concluídas na janela recente, calcula as horas economizadas e, havendo
     * uso e e-mail corporativo, dispara a mensagem. Devolve quantos relatórios foram efetivamente
     * enviados nesta execução — útil para os testes e para o log da tarefa agendada. O instante de
     * referência é recebido por fora para permitir testes determinísticos.</p>
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
            double hoursSaved = completed * hoursSavedPerEvaluation;
            EngagementReportSummary summary = new EngagementReportSummary(
                    empresa.getId(), empresa.getName(), empresa.getCorporateEmail(),
                    periodStart, now, completed, hoursSaved);
            emailSender.sendMonthlyReport(empresa.getCorporateEmail(), empresa.getName(), summary);
            sent++;
        }
        return sent;
    }
}
