package br.com.iforce.praxis.companyprofile.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.companyprofile.dto.CompanyProfileResponse;
import br.com.iforce.praxis.companyprofile.dto.UpdateCompanyProfileRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyProfileServiceTest {

    private static final String EMPRESA_ID = "empresa-1";

    @Mock
    private CurrentEmpresaService currentEmpresaService;
    @Mock
    private EmpresaRepository empresaRepository;
    @Mock
    private AuditEventService auditEventService;

    private CompanyProfileService service;
    private EmpresaEntity empresa;

    @BeforeEach
    void setUp() {
        service = new CompanyProfileService(
                currentEmpresaService,
                empresaRepository,
                auditEventService,
                new AuditMetadata(new ObjectMapper())
        );

        empresa = new EmpresaEntity();
        empresa.setId(EMPRESA_ID);
        empresa.setName("Empresa original");
        empresa.setTradeName("Empresa original");
        empresa.setLegalName("Empresa Original LTDA");
        empresa.setTaxId("12345678000199");
        empresa.setCorporateEmail("antigo@empresa.com");
        empresa.setPhone("+55 21 90000-0000");
        empresa.setWebsite("https://antigo.empresa.com");
        empresa.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        empresa.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));

        when(currentEmpresaService.requiredEmpresaId()).thenReturn(EMPRESA_ID);
        when(empresaRepository.findById(EMPRESA_ID)).thenReturn(Optional.of(empresa));
        lenient().when(empresaRepository.save(any(EmpresaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void updateProfileNormalizesPersistsAndAuditsChangedFields() {
        UpdateCompanyProfileRequest request = new UpdateCompanyProfileRequest(
                "  Empresa atualizada  ",
                "Empresa Atualizada LTDA",
                "12.345.678/0001-90",
                "contato@empresa.com",
                "  ",
                "https://empresa.com"
        );

        CompanyProfileResponse response = service.updateProfile(request);

        assertThat(response.tradeName()).isEqualTo("Empresa atualizada");
        assertThat(response.corporateEmail()).isEqualTo("contato@empresa.com");
        assertThat(response.phone()).isNull();
        assertThat(empresa.getUpdatedAt()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
        verify(empresaRepository).save(empresa);
        verify(auditEventService).auditAdminAction(
                isNull(),
                eq(EMPRESA_ID),
                eq(AuditEventType.COMPANY_PROFILE_UPDATED),
                anyString(),
                anyString()
        );
    }

    @Test
    void updateProfileDoesNotPersistOrAuditWhenNothingChanged() {
        UpdateCompanyProfileRequest request = new UpdateCompanyProfileRequest(
                empresa.getTradeName(),
                empresa.getLegalName(),
                empresa.getTaxId(),
                empresa.getCorporateEmail(),
                empresa.getPhone(),
                empresa.getWebsite()
        );

        CompanyProfileResponse response = service.updateProfile(request);

        assertThat(response.tradeName()).isEqualTo("Empresa original");
        verify(empresaRepository, never()).save(any(EmpresaEntity.class));
        verify(auditEventService, never()).auditAdminAction(
                any(), anyString(), any(), anyString(), anyString()
        );
    }
}
