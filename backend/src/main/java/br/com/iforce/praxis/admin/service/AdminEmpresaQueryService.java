package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.EmpresaAdminPageResponse;
import br.com.iforce.praxis.admin.dto.EmpresaAdminSummaryResponse;
import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.EmpresaStatus;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.billing.persistence.entity.EmpresaCreditBalanceEntity;
import br.com.iforce.praxis.billing.persistence.repository.EmpresaCreditBalanceRepository;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminEmpresaQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final EmpresaRepository empresaRepository;
    private final CandidateAttemptRepository candidateAttemptRepository;
    private final EmpresaCreditBalanceRepository balanceRepository;
    private final int usagePeriodDays;

    public AdminEmpresaQueryService(
            EmpresaRepository empresaRepository,
            CandidateAttemptRepository candidateAttemptRepository,
            EmpresaCreditBalanceRepository balanceRepository,
            @Value("${praxis.admin.usage-period-days:30}") int usagePeriodDays
    ) {
        this.empresaRepository = empresaRepository;
        this.candidateAttemptRepository = candidateAttemptRepository;
        this.balanceRepository = balanceRepository;
        this.usagePeriodDays = usagePeriodDays;
    }

    @Transactional(readOnly = true)
    public EmpresaAdminPageResponse search(
            String search,
            EmpresaStatus status,
            CommercialPlanType plan,
            Instant periodStart,
            Instant periodEnd,
            int page,
            int size
    ) {
        Period period = resolvePeriod(periodStart, periodEnd);
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        String normalizedSearch = normalizeSearch(search);

        Page<EmpresaEntity> result = empresaRepository.searchPage(
                normalizedSearch,
                status,
                plan,
                PageRequest.of(
                        normalizedPage,
                        normalizedSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );

        List<String> empresaIds = result.getContent().stream()
                .map(EmpresaEntity::getId)
                .toList();
        Map<String, Long> usageByEmpresa = usageByEmpresa(empresaIds, period);
        Map<String, Integer> balanceByEmpresa = balanceByEmpresa(empresaIds);

        List<EmpresaAdminSummaryResponse> items = result.getContent().stream()
                .map(empresa -> toSummary(
                        empresa,
                        usageByEmpresa.getOrDefault(empresa.getId(), 0L),
                        balanceByEmpresa.getOrDefault(empresa.getId(), 0)
                ))
                .toList();

        return new EmpresaAdminPageResponse(
                items,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    private Map<String, Long> usageByEmpresa(List<String> empresaIds, Period period) {
        Map<String, Long> usage = new HashMap<>();
        if (empresaIds.isEmpty()) {
            return usage;
        }
        for (Object[] row : candidateAttemptRepository.countByEmpresaIdsAndStatusAndFinishedAtBetween(
                empresaIds,
                AttemptStatus.COMPLETED,
                period.start(),
                period.end()
        )) {
            usage.put((String) row[0], ((Number) row[1]).longValue());
        }
        return usage;
    }

    private Map<String, Integer> balanceByEmpresa(List<String> empresaIds) {
        Map<String, Integer> balances = new HashMap<>();
        if (empresaIds.isEmpty()) {
            return balances;
        }
        for (EmpresaCreditBalanceEntity balance : balanceRepository.findAllById(empresaIds)) {
            balances.put(balance.getEmpresaId(), balance.getBalance());
        }
        return balances;
    }

    private EmpresaAdminSummaryResponse toSummary(EmpresaEntity empresa, long completed, int balance) {
        return new EmpresaAdminSummaryResponse(
                empresa.getId(),
                empresa.getName(),
                empresa.getTradeName(),
                empresa.getTaxId(),
                empresa.getCorporateEmail(),
                empresa.getCommercialPlanType(),
                empresa.getStatus(),
                completed,
                balance,
                empresa.getCreatedAt()
        );
    }

    private Period resolvePeriod(Instant periodStart, Instant periodEnd) {
        Instant end = periodEnd == null ? Instant.now() : periodEnd;
        Instant start = periodStart == null ? end.minus(usagePeriodDays, ChronoUnit.DAYS) : periodStart;
        if (start.isAfter(end)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O início do período não pode ser posterior ao fim."
            );
        }
        return new Period(start, end);
    }

    private String normalizeSearch(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.toLowerCase().trim() + "%";
    }

    private record Period(Instant start, Instant end) {
    }
}
