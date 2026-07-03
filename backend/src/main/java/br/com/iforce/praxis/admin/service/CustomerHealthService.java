package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.EmpresaHealthResponse;

import br.com.iforce.praxis.admin.model.CustomerHealthLevel;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.Comparator;

import java.util.List;


/**
 * Calcula a "Saúde do Cliente" (Health Score) de retenção: compara o volume de avaliações
 * concluídas nos últimos {@code health-period-days} dias com o período imediatamente anterior de
 * mesmo tamanho e classifica cada cliente ativo como saudável, em risco ou sem base suficiente.
 *
 * <p>Na visão do processo, é o motor por trás de duas entregas de retenção proativa: o painel de
 * saúde do ADMIN e a fila de atuação para o time de Customer Success ({@code
 * /api/admin/empresas/at-risk}). Um cliente entra em risco quando a queda de uso ultrapassa o
 * limite configurado (por padrão, mais de 30%) — o gatilho para intervir antes que ele decida
 * cancelar. Clientes sem histórico relevante no período anterior não geram alarme falso.</p>
 */
@Service
public class CustomerHealthService {

    private final EmpresaRepository empresaRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final int periodDays;
    private final double atRiskDropThreshold;
    private final long minBaselineCompletions;

    public CustomerHealthService(
            EmpresaRepository empresaRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            @Value("${praxis.retention.health-period-days:30}") int periodDays,
            @Value("${praxis.retention.at-risk-drop-threshold:0.30}") double atRiskDropThreshold,
            @Value("${praxis.retention.min-baseline-completions:3}") long minBaselineCompletions
    ) {
        this.empresaRepository = empresaRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.periodDays = periodDays;
        this.atRiskDropThreshold = atRiskDropThreshold;
        this.minBaselineCompletions = minBaselineCompletions;
    }

    /**
     * Fila de atuação para Customer Success: clientes ativos cuja utilização caiu além do limite.
     *
     * <p>Percorre apenas os clientes com acesso liberado ({@link EmpresaStatus#ATIVO}) — os que
     * mais interessa reter — calcula a saúde de cada um e devolve os que estão em risco, ordenados
     * da maior queda para a menor, para que o time priorize quem está evadindo mais rápido.</p>
     *
     * @param now instante de referência das janelas de 30 dias (injetável para testes determinísticos)
     * @return clientes em risco, do mais crítico ao menos crítico
     */
    @Transactional(readOnly = true)
    public List<EmpresaHealthResponse> atRiskEmpresas(Instant now) {
        return empresaRepository.findByStatuses(List.of(EmpresaStatus.ATIVO)).stream()
                .map(empresa -> healthFor(empresa, now))
                .filter(health -> health.level() == CustomerHealthLevel.AT_RISK)
                .sorted(Comparator.comparing(EmpresaHealthResponse::dropPercent).reversed())
                .toList();
    }

    /**
     * Calcula a saúde de retenção de um cliente comparando duas janelas de mesmo tamanho.
     *
     * <p>Fluxo do cálculo: conta as avaliações concluídas na janela atual ({@code now - N dias} até
     * {@code now}) e na janela anterior ({@code now - 2N dias} até {@code now - N dias}). Se o
     * cliente não tinha uso relevante antes (base menor que {@code min-baseline-completions}), a
     * queda não é confiável e ele é marcado como {@link CustomerHealthLevel#NO_BASELINE}. Caso
     * contrário, mede-se a queda relativa e o índice de saúde: acima do limite de queda o cliente
     * fica {@link CustomerHealthLevel#AT_RISK}; caso contrário, {@link CustomerHealthLevel#HEALTHY}.</p>
     *
     * @param empresa cliente a avaliar
     * @param now     instante de referência das janelas
     * @return a saúde consolidada do cliente
     */
    @Transactional(readOnly = true)
    public EmpresaHealthResponse healthFor(EmpresaEntity empresa, Instant now) {
        Instant currentStart = now.minus(periodDays, ChronoUnit.DAYS);
        Instant previousStart = now.minus(2L * periodDays, ChronoUnit.DAYS);

        long current = candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtBetween(
                empresa.getId(), AttemptStatus.COMPLETED, currentStart, now);
        long previous = candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtBetween(
                empresa.getId(), AttemptStatus.COMPLETED, previousStart, currentStart);

        CustomerHealthLevel level;
        Double dropPercent;
        Integer healthScore;
        if (previous < minBaselineCompletions) {
            level = CustomerHealthLevel.NO_BASELINE;
            dropPercent = null;
            healthScore = null;
        } else {
            double drop = (double) (previous - current) / (double) previous;
            dropPercent = drop;
            double retentionRatio = Math.min(1.0, (double) current / (double) previous);
            healthScore = (int) Math.round(retentionRatio * 100);
            level = drop > atRiskDropThreshold ? CustomerHealthLevel.AT_RISK : CustomerHealthLevel.HEALTHY;
        }

        Instant lastCompletedAt = candidateAttemptRepository
                .findLastFinishedAt(empresa.getId(), AttemptStatus.COMPLETED)
                .orElse(null);

        return new EmpresaHealthResponse(
                empresa.getId(),
                empresa.getName(),
                empresa.getTradeName(),
                empresa.getCorporateEmail(),
                empresa.getCommercialPlanType(),
                empresa.getStatus(),
                level,
                current,
                previous,
                dropPercent,
                healthScore,
                lastCompletedAt,
                currentStart,
                now,
                previousStart,
                currentStart
        );
    }
}
