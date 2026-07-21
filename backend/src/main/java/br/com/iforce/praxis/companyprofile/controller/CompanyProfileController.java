package br.com.iforce.praxis.companyprofile.controller;

import br.com.iforce.praxis.companyprofile.dto.CompanyProfileResponse;
import br.com.iforce.praxis.companyprofile.dto.UpdateCompanyProfileRequest;
import br.com.iforce.praxis.companyprofile.service.CompanyProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API do perfil cadastral da empresa autenticada.
 */
@RestController
@RequestMapping("/api/v1/company-profile")
@Tag(name = "Company Profile", description = "Perfil cadastral da empresa autenticada.")
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    public CompanyProfileController(CompanyProfileService companyProfileService) {
        this.companyProfileService = companyProfileService;
    }

    @GetMapping
    @Operation(
            summary = "Carrega o perfil da empresa",
            description = "Retorna dados cadastrais visíveis para a empresa autenticada."
    )
    public ResponseEntity<CompanyProfileResponse> getProfile() {
        return ResponseEntity.ok(companyProfileService.getProfile());
    }

    @PutMapping
    @Operation(
            summary = "Atualiza o perfil da empresa",
            description = "Atualiza os dados cadastrais da própria empresa e registra a alteração na auditoria."
    )
    public ResponseEntity<CompanyProfileResponse> updateProfile(
            @Valid @RequestBody UpdateCompanyProfileRequest request
    ) {
        return ResponseEntity.ok(companyProfileService.updateProfile(request));
    }
}
