package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.billing.dto.CreditCapacityResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreditCapacityServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;
    @Mock
    private CreditService creditService;

    private CreditCapacityService service;

    @BeforeEach
    void setUp() {
        service = new CreditCapacityService(
                empresaRepository,
                candidateAttemptRepository,
                creditService
        );
    }

    @Test
    void subtractsOpenAttemptReservationsFromBalance() {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("tenant-1");
        empresa.setCommercialPlanType(CommercialPlanType.AVULSO);
        when(empresaRepository.findById("tenant-1")).thenReturn(Optional.of(empresa));
        when(creditService.getBalance("tenant-1")).thenReturn(5);
        when(candidateAttemptRepository.countByEmpresaIdAndStatusIn(
                "tenant-1",
                List.of(AttemptStatus.NOT_STARTED, AttemptStatus.IN_PROGRESS)
        )).thenReturn(2L);

        CreditCapacityResponse response = service.getCapacity("tenant-1");

        assertThat(response.metered()).isTrue();
        assertThat(response.creditBalance()).isEqualTo(5);
        assertThat(response.reservedCredits()).isEqualTo(2);
        assertThat(response.availableCredits()).isEqualTo(3);
    }
}
