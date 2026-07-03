package br.com.iforce.praxis.engagement.service;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.engagement.dto.EngagementReportSummary;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class EngagementReportServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");
    private static final Instant PERIOD_START = NOW.minus(30, ChronoUnit.DAYS);

    @Mock private EmpresaRepository empresaRepository;
    @Mock private CandidateAttemptRepository candidateAttemptRepository;
    @Mock private EngagementReportEmailSender emailSender;

    private EngagementReportService service;

    @BeforeEach
    void setUp() {
        service = new EngagementReportService(empresaRepository, candidateAttemptRepository, emailSender, 30, 1.5);
    }

    private EmpresaEntity empresa(String id, String corporateEmail) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(id);
        empresa.setName("Cliente " + id);
        empresa.setCorporateEmail(corporateEmail);
        empresa.setStatus(EmpresaStatus.ATIVO);
        return empresa;
    }

    private void stubCompleted(String id, long completed) {
        when(candidateAttemptRepository.countByEmpresaIdAndStatusAndFinishedAtBetween(
                id, AttemptStatus.COMPLETED, PERIOD_START, NOW)).thenReturn(completed);
    }

    @Test
    void sendsMonthlyReportWithHoursSavedToCorporateEmail() {
        when(empresaRepository.findByStatuses(List.of(EmpresaStatus.ATIVO)))
                .thenReturn(List.of(empresa("t1", "ops@acme.com")));
        stubCompleted("t1", 8);

        int sent = service.sendMonthlyReports(NOW);

        assertThat(sent).isEqualTo(1);
        ArgumentCaptor<EngagementReportSummary> captor = ArgumentCaptor.forClass(EngagementReportSummary.class);
        verify(emailSender).sendMonthlyReport(eq("ops@acme.com"), eq("Cliente t1"), captor.capture());
        EngagementReportSummary summary = captor.getValue();
        assertThat(summary.completedEvaluations()).isEqualTo(8);
        assertThat(summary.hoursSaved()).isEqualTo(12.0); // 8 x 1.5
    }

    @Test
    void skipsEmpresaWithoutUsage() {
        when(empresaRepository.findByStatuses(List.of(EmpresaStatus.ATIVO)))
                .thenReturn(List.of(empresa("t1", "ops@acme.com")));
        stubCompleted("t1", 0);

        int sent = service.sendMonthlyReports(NOW);

        assertThat(sent).isZero();
        verify(emailSender, never()).sendMonthlyReport(anyString(), anyString(), any());
    }

    @Test
    void skipsEmpresaWithoutCorporateEmail() {
        when(empresaRepository.findByStatuses(List.of(EmpresaStatus.ATIVO)))
                .thenReturn(List.of(empresa("t1", null)));
        stubCompleted("t1", 5);

        int sent = service.sendMonthlyReports(NOW);

        assertThat(sent).isZero();
        verify(emailSender, never()).sendMonthlyReport(anyString(), anyString(), any());
    }
}
