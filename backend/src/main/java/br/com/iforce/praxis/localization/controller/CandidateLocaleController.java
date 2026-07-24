package br.com.iforce.praxis.localization.controller;

import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleSelectionRequest;
import br.com.iforce.praxis.localization.dto.SimulationLocalizationDtos.LocaleSelectionResponse;
import br.com.iforce.praxis.localization.service.CandidateContentLocalizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/candidate/attempts/{token}/locale")
@Tag(name = "Candidate locale", description = "Seleção persistida de idioma da participação.")
public class CandidateLocaleController {

    private final CandidateContentLocalizationService localizationService;

    public CandidateLocaleController(CandidateContentLocalizationService localizationService) {
        this.localizationService = localizationService;
    }

    @GetMapping("/available")
    @Operation(summary = "Lista idiomas aprovados disponíveis para a versão da participação")
    public ResponseEntity<List<String>> available(@PathVariable String token) {
        return ResponseEntity.ok(localizationService.availableLocales(token));
    }

    @PostMapping
    @Operation(summary = "Persiste idioma escolhido no convite, ATS ou pela pessoa candidata")
    public ResponseEntity<LocaleSelectionResponse> select(
            @PathVariable String token,
            @Valid @RequestBody LocaleSelectionRequest request
    ) {
        return ResponseEntity.ok(localizationService.selectLocale(token, request));
    }
}
