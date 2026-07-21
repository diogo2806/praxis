package br.com.iforce.praxis.companyprofile.service;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.companyprofile.dto.CompanyProfileResponse;
import br.com.iforce.praxis.companyprofile.dto.UpdateCompanyProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.springframework.http.HttpStatus.NOT_FOUND;

/**
 * Consulta e atualiza os dados cadastrais da empresa autenticada.
 *
 * <p>A operação permanece isolada pela empresa da sessão. As alterações são
 * registradas na trilha append-only de auditoria sem gravar valores sensíveis
 * nos metadados.</p>
 */
@Service
public class CompanyProfileService {

    private final CurrentEmpresaService currentEmpresaService;
    private final EmpresaRepository empresaRepository;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;

    public CompanyProfileService(
            CurrentEmpresaService currentEmpresaService,
            EmpresaRepository empresaRepository,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.empresaRepository = empresaRepository;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
    }

    @Transactional(readOnly = true)
    public CompanyProfileResponse getProfile() {
        return toResponse(requiredEmpresa());
    }

    @Transactional
    public CompanyProfileResponse updateProfile(UpdateCompanyProfileRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        EmpresaEntity empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Empresa não encontrada."));

        String tradeName = request.tradeName().trim();
        String legalName = normalizeNullable(request.legalName());
        String taxId = normalizeNullable(request.taxId());
        String corporateEmail = normalizeNullable(request.corporateEmail());
        String phone = normalizeNullable(request.phone());
        String website = normalizeNullable(request.website());

        List<String> changedFields = new ArrayList<>();
        trackChange(changedFields, "tradeName", empresa.getTradeName(), tradeName);
        trackChange(changedFields, "legalName", empresa.getLegalName(), legalName);
        trackChange(changedFields, "taxId", empresa.getTaxId(), taxId);
        trackChange(changedFields, "corporateEmail", empresa.getCorporateEmail(), corporateEmail);
        trackChange(changedFields, "phone", empresa.getPhone(), phone);
        trackChange(changedFields, "website", empresa.getWebsite(), website);

        if (changedFields.isEmpty()) {
            return toResponse(empresa);
        }

        empresa.setTradeName(tradeName);
        empresa.setLegalName(legalName);
        empresa.setTaxId(taxId);
        empresa.setCorporateEmail(corporateEmail);
        empresa.setPhone(phone);
        empresa.setWebsite(website);
        empresa.setUpdatedAt(Instant.now());
        empresaRepository.save(empresa);

        auditEventService.auditAdminAction(
                null,
                empresaId,
                AuditEventType.COMPANY_PROFILE_UPDATED,
                "Dados cadastrais da empresa atualizados pelo perfil EMPRESA.",
                auditMetadata.of("fields", List.copyOf(changedFields), "source", "company-profile")
        );

        return toResponse(empresa);
    }

    private EmpresaEntity requiredEmpresa() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Empresa não encontrada."));
    }

    private CompanyProfileResponse toResponse(EmpresaEntity empresa) {
        return new CompanyProfileResponse(
                fallback(empresa.getTradeName(), empresa.getName()),
                empresa.getLegalName(),
                empresa.getTaxId(),
                empresa.getCorporateEmail(),
                empresa.getPhone(),
                empresa.getWebsite()
        );
    }

    private static void trackChange(List<String> changedFields, String field, String current, String updated) {
        if (!Objects.equals(normalizeNullable(current), normalizeNullable(updated))) {
            changedFields.add(field);
        }
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
