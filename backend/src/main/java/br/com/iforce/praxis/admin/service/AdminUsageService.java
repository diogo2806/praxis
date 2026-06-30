package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.EmpresaUsageResponse;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;

import java.time.temporal.ChronoUnit;


/**
 * Calcula o uso de um cliente no painel administrativo.
 *
 * <p>Na Parte A, uso significa quantidade de avaliações concluídas
 * ({@code CandidateAttemptEntity} com {@code status = COMPLETED}) cujo {@code finishedAt}
 * cai dentro do período consultado. Nenhuma tabela de medição é criada: a contagem é feita
 * diretamente sobre as tentativas.</p>
 */
@Service
public class AdminUsageService {

    private final CandidateAttemptRepository candidateAttemptRepository;

    public AdminUsageService(CandidateAttemptRepository candidateAttemptRepository) {
        this.candidateAttemptRepository = candidateAttemptRepository;
    }

    /** Total de avaliações concluídas de um empresa dentro do período informado. */
    @Transactional(readOnly = true)
    public long countCompletedInPeriod(String empresaId, Instant periodStart, Instant periodEnd) {
        return candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtBetween(
                empresaId, AttemptStatus.COMPLETED, periodStart, periodEnd);
    }

    /** Detalhe de uso para a aba "Uso" do cliente. */
    @Transactional(readOnly = true)
    public EmpresaUsageResponse usage(String empresaId, Instant periodStart, Instant periodEnd) {
        Instant now = Instant.now();
        long inPeriod = countCompletedInPeriod(empresaId, periodStart, periodEnd);
        long last7 = candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtAfter(
                empresaId, AttemptStatus.COMPLETED, now.minus(7, ChronoUnit.DAYS));
        long last30 = candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtAfter(
                empresaId, AttemptStatus.COMPLETED, now.minus(30, ChronoUnit.DAYS));
        long allTime = candidateAttemptRepository.countByEmpresaIdAndStatus(
                empresaId, AttemptStatus.COMPLETED);
        Instant lastCompleted = candidateAttemptRepository
                .findLastFinishedAt(empresaId, AttemptStatus.COMPLETED)
                .orElse(null);

        return new EmpresaUsageResponse(
                empresaId, periodStart, periodEnd, inPeriod, last7, last30, allTime, lastCompleted);
    }
}
