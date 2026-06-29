package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.AdminDashboardResponse;
import br.com.iforce.praxis.admin.dto.TenantAdminSummaryResponse;
import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
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

    private final TenantRepository tenantRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final AdminUsageService adminUsageService;
    private final int usagePeriodDays;

    public AdminDashboardService(
            TenantRepository tenantRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            AdminUsageService adminUsageService,
            @Value("${praxis.admin.usage-period-days:30}") int usagePeriodDays
    ) {
        this.tenantRepository = tenantRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.adminUsageService = adminUsageService;
        this.usagePeriodDays = usagePeriodDays;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse dashboard(Instant periodStart, Instant periodEnd) {
        Instant end = periodEnd == null ? Instant.now() : periodEnd;
        Instant start = periodStart == null ? end.minus(usagePeriodDays, ChronoUnit.DAYS) : periodStart;

        long totalCompleted = candidateAttemptRepository.countByStatusAndFinishedAtBetween(
                AttemptStatus.COMPLETED, start, end);

        List<AdminDashboardResponse.TopUsageTenant> topUsage = candidateAttemptRepository
                .findTopUsageTenants(AttemptStatus.COMPLETED, start, end, PageRequest.of(0, TOP_USAGE_LIMIT))
                .stream()
                .filter(row -> !"PLATFORM".equals((String) row[0]))
                .map(row -> {
                    String tenantId = (String) row[0];
                    long count = ((Number) row[1]).longValue();
                    String name = tenantRepository.findById(tenantId)
                            .map(TenantEntity::getName)
                            .orElse(tenantId);
                    return new AdminDashboardResponse.TopUsageTenant(tenantId, name, count);
                })
                .toList();

        List<TenantAdminSummaryResponse> recent = tenantRepository
                .findRecentClients(PageRequest.of(0, RECENT_LIMIT)).stream()
                .map(tenant -> toSummary(tenant, start, end))
                .toList();

        List<TenantAdminSummaryResponse> attention = tenantRepository
                .findByStatuses(List.of(TenantStatus.SUSPENSO, TenantStatus.CANCELADO)).stream()
                .map(tenant -> toSummary(tenant, start, end))
                .toList();

        return new AdminDashboardResponse(
                start,
                end,
                tenantRepository.countClients(),
                tenantRepository.countClientsByStatus(TenantStatus.ATIVO),
                tenantRepository.countClientsByStatus(TenantStatus.EM_TESTE),
                tenantRepository.countClientsByStatus(TenantStatus.SUSPENSO),
                tenantRepository.countClientsByStatus(TenantStatus.CANCELADO),
                totalCompleted,
                topUsage,
                recent,
                attention
        );
    }

    private TenantAdminSummaryResponse toSummary(TenantEntity tenant, Instant start, Instant end) {
        return new TenantAdminSummaryResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getTradeName(),
                tenant.getTaxId(),
                tenant.getCorporateEmail(),
                tenant.getCommercialPlanType(),
                tenant.getStatus(),
                adminUsageService.countCompletedInPeriod(tenant.getId(), start, end),
                tenant.getCreatedAt()
        );
    }
}
