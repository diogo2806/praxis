package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.EmpresaHealthResponse;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.CustomerHealthLevel;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.List;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class CustomerHealthServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant CURRENT_START = NOW.minus(30, ChronoUnit.DAYS);
    private static final Instant PREVIOUS_START = NOW.minus(60, ChronoUnit.DAYS);

    @Mock private EmpresaRepository empresaRepository;
    @Mock private CandidateAttemptRepository candidateAttemptRepository;

    private CustomerHealthService service;

    @BeforeEach
    void setUp() {
        service = new CustomerHealthService(empresaRepository, candidateAttemptRepository, 30, 0.30, 3);
    }

    private EmpresaEntity empresa(String id) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(id);
        empresa.setName("Cliente " + id);
        empresa.setCorporateEmail(id + "@cliente.com");
        empresa.setStatus(EmpresaStatus.ATIVO);
        empresa.setCommercialPlanType(CommercialPlanType.PROFISSIONAL);
        return empresa;
    }

    private void stubCounts(String id, long previous, long current) {
        when(candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtBetween(
                id, AttemptStatus.COMPLETED, CURRENT_START, NOW)).thenReturn(current);
        when(candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtBetween(
                id, AttemptStatus.COMPLETED, PREVIOUS_START, CURRENT_START)).thenReturn(previous);
        when(candidateAttemptRepository.findLastFinishedAt(id, AttemptStatus.COMPLETED))
                .thenReturn(Optional.empty());
    }

    @Test
    void flagsAtRiskWhenUsageDropsBeyondThreshold() {
        stubCounts("t1", 10, 5);

        EmpresaHealthResponse health = service.healthFor(empresa("t1"), NOW);

        assertThat(health.level()).isEqualTo(CustomerHealthLevel.AT_RISK);
        assertThat(health.completedCurrentPeriod()).isEqualTo(5);
        assertThat(health.completedPreviousPeriod()).isEqualTo(10);
        assertThat(health.dropPercent()).isEqualTo(0.5);
        assertThat(health.healthScore()).isEqualTo(50);
    }

    @Test
    void keepsHealthyWhenUsageStableOrGrowing() {
        stubCounts("t1", 10, 12);

        EmpresaHealthResponse health = service.healthFor(empresa("t1"), NOW);

        assertThat(health.level()).isEqualTo(CustomerHealthLevel.HEALTHY);
        assertThat(health.dropPercent()).isNegative();
        assertThat(health.healthScore()).isEqualTo(100);
    }

    @Test
    void marksNoBaselineWhenPreviousPeriodTooSmall() {
        stubCounts("t1", 2, 0);

        EmpresaHealthResponse health = service.healthFor(empresa("t1"), NOW);

        assertThat(health.level()).isEqualTo(CustomerHealthLevel.NO_BASELINE);
        assertThat(health.dropPercent()).isNull();
        assertThat(health.healthScore()).isNull();
    }

    @Test
    void atRiskQueueReturnsOnlyAtRiskClientsSortedByDeepestDrop() {
        when(empresaRepository.findByStatuses(List.of(EmpresaStatus.ATIVO)))
                .thenReturn(List.of(empresa("t1"), empresa("t2"), empresa("t3")));
        stubCounts("t1", 10, 3); // queda de 70%
        stubCounts("t2", 10, 6); // queda de 40%
        stubCounts("t3", 10, 10); // estável

        List<EmpresaHealthResponse> atRisk = service.atRiskEmpresas(NOW);

        assertThat(atRisk).extracting(EmpresaHealthResponse::empresaId).containsExactly("t1", "t2");
        assertThat(atRisk.get(0).dropPercent()).isGreaterThan(atRisk.get(1).dropPercent());
    }
}
