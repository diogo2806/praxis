package br.com.iforce.praxis.auth.service;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthVerticalServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Test
    void trueWhenTenantHasHealthVerticalEnabled() {
        TenantEntity tenant = new TenantEntity();
        tenant.setHealthVertical(true);
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));

        assertThat(new HealthVerticalService(tenantRepository).isHealthVertical("tenant-1")).isTrue();
    }

    @Test
    void falseWhenTenantHasHealthVerticalDisabled() {
        TenantEntity tenant = new TenantEntity();
        tenant.setHealthVertical(false);
        when(tenantRepository.findById("tenant-1")).thenReturn(Optional.of(tenant));

        assertThat(new HealthVerticalService(tenantRepository).isHealthVertical("tenant-1")).isFalse();
    }

    @Test
    void falseWhenTenantMissing() {
        when(tenantRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThat(new HealthVerticalService(tenantRepository).isHealthVertical("ghost")).isFalse();
    }

    @Test
    void falseAndNoLookupWhenTenantIdBlank() {
        HealthVerticalService service = new HealthVerticalService(tenantRepository);

        assertThat(service.isHealthVertical("")).isFalse();
        assertThat(service.isHealthVertical(null)).isFalse();
        verifyNoInteractions(tenantRepository);
    }
}
