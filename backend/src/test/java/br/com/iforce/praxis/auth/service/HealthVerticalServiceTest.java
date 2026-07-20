package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthVerticalServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Test
    void trueOnlyWhenFlagAndFormalApprovalExist() {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setHealthVertical(true);
        empresa.setHealthComplianceApprovedAt(Instant.now());
        empresa.setHealthComplianceApprovedBy("user-1");
        when(empresaRepository.findById("empresa-1")).thenReturn(Optional.of(empresa));

        assertThat(new HealthVerticalService(empresaRepository).isHealthVertical("empresa-1")).isTrue();
    }

    @Test
    void falseWhenFlagExistsWithoutFormalApproval() {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setHealthVertical(true);
        when(empresaRepository.findById("empresa-1")).thenReturn(Optional.of(empresa));

        assertThat(new HealthVerticalService(empresaRepository).isHealthVertical("empresa-1")).isFalse();
    }

    @Test
    void falseWhenEmpresaHasHealthVerticalDisabled() {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setHealthVertical(false);
        when(empresaRepository.findById("empresa-1")).thenReturn(Optional.of(empresa));

        assertThat(new HealthVerticalService(empresaRepository).isHealthVertical("empresa-1")).isFalse();
    }

    @Test
    void falseWhenEmpresaMissing() {
        when(empresaRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThat(new HealthVerticalService(empresaRepository).isHealthVertical("ghost")).isFalse();
    }

    @Test
    void falseAndNoLookupWhenEmpresaIdBlank() {
        HealthVerticalService service = new HealthVerticalService(empresaRepository);

        assertThat(service.isHealthVertical("")).isFalse();
        assertThat(service.isHealthVertical(null)).isFalse();
        verifyNoInteractions(empresaRepository);
    }
}
