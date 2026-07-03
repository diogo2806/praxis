package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.AdminDashboardResponse;

import br.com.iforce.praxis.admin.dto.EmpresaAdminSummaryResponse;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.service.CreditService;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.domain.PageRequest;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.List;


/**
 * Consolida os indicadores do dashboard administrativo (rota {@code /admin}): contagem de
 * clientes por status, uso total no período, ranking de uso, clientes recentes e a lista de
 * clientes que exigem atenção.
 */
@Service
public class AdminDashboardService {

    private static final int TOP_USAGE_LIMIT = 5;
    private static final int RECENT_LIMIT = 5;

    private final EmpresaRepository empresaRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AdminUsageService adminUsageService;
    private final CreditService creditService;
    private final int usagePeriodDays;

    public AdminDashboardService(
            EmpresaRepository empresaRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            AdminUsageService adminUsageService,
            CreditService creditService,
            @Value("${praxis.admin.usage-period-days:30}") int usagePeriodDays
    ) {
        this.empresaRepository = empresaRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.adminUsageService = adminUsageService;
        this.creditService = creditService;
        this.usagePeriodDays = usagePeriodDays;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard(Instant periodStart, Instant periodEnd) {
        Instant end = periodEnd == null ? Instant.now() : periodEnd;
        Instant start = periodStart == null ? end.minus(usagePeriodDays, ChronoUnit.DAYS) : periodStart;

        long totalCompleted = candidateAttemptRepository.countByStatusAndFinishedAtBetween(
                AttemptStatus.COMPLETED, start, end);

        List<AdminDashboardResponse.TopUsageEmpresa> topUsage = candidateAttemptRepository
                .findTopUsageEmpresas(AttemptStatus.COMPLETED, start, end, PageRequest.of(0, TOP_USAGE_LIMIT))
                .stream()
                .filter(row -> !"PLATFORM".equals((String) row[0]))
                .map(row -> {
                    String empresaId = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    String name = empresaRepository.findById(empresaId)
                            .map(EmpresaEntity::getName)
                            .orElse(empresaId);
                    return new AdminDashboardResponse.TopUsageEmpresa(empresaId, name, count);
                })
                .toList();

        List<EmpresaAdminSummaryResponse> recent = empresaRepository
                .findRecentClients(PageRequest.of(0, RECENT_LIMIT)).stream()
                .map(empresa -> toSummary(empresa, start, end))
                .toList();

        List<EmpresaAdminSummaryResponse> attention = empresaRepository
                .findByStatuses(List.of(EmpresaStatus.SUSPENSO, EmpresaStatus.CANCELADO)).stream()
                .map(empresa -> toSummary(empresa, start, end))
                .toList();

        return new AdminDashboardResponse(
                start,
                end,
                empresaRepository.countClients(),
                empresaRepository.countClientsByStatus(EmpresaStatus.ATIVO),
                empresaRepository.countClientsByStatus(EmpresaStatus.EM_TESTE),
                empresaRepository.countClientsByStatus(EmpresaStatus.SUSPENSO),
                empresaRepository.countClientsByStatus(EmpresaStatus.CANCELADO),
                totalCompleted,
                topUsage,
                recent,
                attention
        );
    }

    private EmpresaAdminSummaryResponse toSummary(EmpresaEntity empresa, Instant start, Instant end) {
        return new EmpresaAdminSummaryResponse(
                empresa.getId(),
                empresa.getName(),
                empresa.getTradeName(),
                empresa.getTaxId(),
                empresa.getCorporateEmail(),
                empresa.getCommercialPlanType(),
                empresa.getStatus(),
                adminUsageService.countCompletedInPeriod(empresa.getId(), start, end),
                creditService.getBalance(empresa.getId()),
                empresa.getCreatedAt()
        );
    }
}
