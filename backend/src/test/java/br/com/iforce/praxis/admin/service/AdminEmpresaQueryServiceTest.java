package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.EmpresaAdminPageResponse;
import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.EmpresaStatus;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.billing.persistence.entity.EmpresaCreditBalanceEntity;
import br.com.iforce.praxis.billing.persistence.repository.EmpresaCreditBalanceRepository;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEmpresaQueryServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private EmpresaCreditBalanceRepository balanceRepository;

    private AdminEmpresaQueryService service;

    @BeforeEach
    void setUp() {
        service = new AdminEmpresaQueryService(
                empresaRepository,
                candidateAttemptRepository,
                balanceRepository,
                30
        );
    }

    @Test
    void searchAggregatesUsageAndBalancesInBatch() {
        Instant start = Instant.parse("2026-06-01T00:00:00Z");
        Instant end = Instant.parse("2026-07-01T00:00:00Z");
        EmpresaEntity alpha = empresa("tnt_alpha", "Alpha", Instant.parse("2026-06-20T00:00:00Z"));
        EmpresaEntity beta = empresa("tnt_beta", "Beta", Instant.parse("2026-06-10T00:00:00Z"));

        when(empresaRepository.searchPage(eq("%acme%"), eq(EmpresaStatus.ATIVO),
                eq(CommercialPlanType.PROFISSIONAL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(alpha, beta), PageRequest.of(0, 100), 2));
        when(candidateAttemptRepository.countByEmpresaIdsAndStatusAndFinishedAtBetween(
                List.of("tnt_alpha", "tnt_beta"), AttemptStatus.COMPLETED, start, end))
                .thenReturn(List.<Object[]>of(new Object[]{"tnt_alpha", 7L}));
        when(balanceRepository.findAllById(List.of("tnt_alpha", "tnt_beta")))
                .thenReturn(List.of(balance("tnt_beta", 12)));

        EmpresaAdminPageResponse response = service.search(
                " Acme ",
                EmpresaStatus.ATIVO,
                CommercialPlanType.PROFISSIONAL,
                start,
                end,
                -3,
                500
        );

        assertThat(response.page()).isZero();
        assertThat(response.size()).isEqualTo(100);
        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).completedAttemptsInPeriod()).isEqualTo(7);
        assertThat(response.items().get(0).creditBalance()).isZero();
        assertThat(response.items().get(1).completedAttemptsInPeriod()).isZero();
        assertThat(response.items().get(1).creditBalance()).isEqualTo(12);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(empresaRepository).searchPage(
                eq("%acme%"),
                eq(EmpresaStatus.ATIVO),
                eq(CommercialPlanType.PROFISSIONAL),
                pageable.capture()
        );
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    void searchRejectsInvertedPeriod() {
        Instant start = Instant.parse("2026-07-02T00:00:00Z");
        Instant end = Instant.parse("2026-07-01T00:00:00Z");

        assertThatThrownBy(() -> service.search(null, null, null, start, end, 0, 20))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);
    }

    private EmpresaEntity empresa(String id, String name, Instant createdAt) {
        EmpresaEntity entity = new EmpresaEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setCommercialPlanType(CommercialPlanType.PROFISSIONAL);
        entity.setStatus(EmpresaStatus.ATIVO);
        entity.setCreatedAt(createdAt);
        entity.setUpdatedAt(createdAt);
        return entity;
    }

    private EmpresaCreditBalanceEntity balance(String empresaId, int value) {
        EmpresaCreditBalanceEntity entity = new EmpresaCreditBalanceEntity();
        entity.setEmpresaId(empresaId);
        entity.setBalance(value);
        entity.setUpdatedAt(Instant.parse("2026-07-01T00:00:00Z"));
        return entity;
    }
}
