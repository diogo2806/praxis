package br.com.iforce.praxis.companyprofile.controller;

import br.com.iforce.praxis.companyprofile.dto.CompanyProfileResponse;
import br.com.iforce.praxis.companyprofile.service.CompanyProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
            description = "Retorna apenas dados cadastrais visiveis para o cliente, sem identificadores tecnicos."
    )
    public ResponseEntity<CompanyProfileResponse> getProfile() {
        return ResponseEntity.ok(companyProfileService.getProfile());
    }
}
