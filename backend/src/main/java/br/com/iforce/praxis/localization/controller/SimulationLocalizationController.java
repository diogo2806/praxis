package br.com.iforce.praxis.localization.controller;

import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.ConfigureLocalesRequest;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.ExportLocalePackageResponse;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.ImportLocalePackageRequest;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleContentRequest;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleContentResponse;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleSummaryResponse;
import br.com.iforce.praxis.localization.service.SimulationLocalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/simulation-versions/{versionId}/locales")
@Tag(name = "Simulation localization", description = "Conteúdo multilíngue sem alteração de grafo, peso ou pontuação.")
public class SimulationLocalizationController {

    private final SimulationLocalizationService localizationService;

    public SimulationLocalizationController(SimulationLocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @PostMapping("/configure")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR')")
    @Operation(summary = "Define idioma base e idiomas habilitados na versão")
    public ResponseEntity<List<LocaleSummaryResponse>> configure(
            @PathVariable long versionId,
            @Valid @RequestBody ConfigureLocalesRequest request
    ) {
        return ResponseEntity.ok(localizationService.configure(versionId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR','RESULTS_ANALYST','PARTNER_SPECIALIST')")
    @Operation(summary = "Lista status, completude e revisão de cada idioma")
    public ResponseEntity<List<LocaleSummaryResponse>> list(@PathVariable long versionId) {
        return ResponseEntity.ok(localizationService.list(versionId));
    }

    @GetMapping("/{locale}")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR','PARTNER_SPECIALIST')")
    public ResponseEntity<LocaleContentResponse> get(
            @PathVariable long versionId,
            @PathVariable String locale
    ) {
        return ResponseEntity.ok(localizationService.get(versionId, locale));
    }

    @PutMapping("/{locale}")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR','PARTNER_SPECIALIST')")
    @Operation(summary = "Salva tradução textual e acessível preservando IDs estruturais")
    public ResponseEntity<LocaleContentResponse> save(
            @PathVariable long versionId,
            @PathVariable String locale,
            @Valid @RequestBody LocaleContentRequest request
    ) {
        return ResponseEntity.ok(localizationService.save(versionId, locale, request));
    }

    @PostMapping("/{locale}/review")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR','PARTNER_SPECIALIST')")
    @Operation(summary = "Envia tradução estruturalmente válida para revisão")
    public ResponseEntity<LocaleContentResponse> review(
            @PathVariable long versionId,
            @PathVariable String locale
    ) {
        return ResponseEntity.ok(localizationService.submitForReview(versionId, locale));
    }

    @PostMapping("/{locale}/approve")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER')")
    @Operation(summary = "Aprova idioma para uso por participantes e relatórios")
    public ResponseEntity<LocaleContentResponse> approve(
            @PathVariable long versionId,
            @PathVariable String locale
    ) {
        return ResponseEntity.ok(localizationService.approve(versionId, locale));
    }

    @GetMapping("/{locale}/export")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR','PARTNER_SPECIALIST')")
    @Operation(summary = "Exporta pacote JSON com IDs estruturais preservados")
    public ResponseEntity<ExportLocalePackageResponse> exportPackage(
            @PathVariable long versionId,
            @PathVariable String locale
    ) {
        return ResponseEntity.ok(localizationService.exportPackage(versionId, locale));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('EMPRESA','TEAM_MANAGER','ASSESSMENT_EDITOR','PARTNER_SPECIALIST')")
    @Operation(summary = "Importa pacote de idioma e valida equivalência estrutural")
    public ResponseEntity<LocaleContentResponse> importPackage(
            @PathVariable long versionId,
            @Valid @RequestBody ImportLocalePackageRequest request
    ) {
        return ResponseEntity.ok(localizationService.importPackage(versionId, request));
    }
}
