package br.com.iforce.praxis.term.controller;

import br.com.iforce.praxis.term.dto.AcceptTermRequest;
import br.com.iforce.praxis.term.dto.TermAcceptanceStatusResponse;
import br.com.iforce.praxis.term.dto.TermResponse;
import br.com.iforce.praxis.term.service.TermAcceptanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/terms")
@Tag(name = "Termos", description = "Termo de responsabilidade do recrutador e registro de aceite (REQ-L5).")
public class TermController {

    private final TermAcceptanceService termAcceptanceService;

    public TermController(TermAcceptanceService termAcceptanceService) {
        this.termAcceptanceService = termAcceptanceService;
    }

    @GetMapping("/responsibility")
    @Operation(summary = "Texto e versão do termo de responsabilidade")
    public ResponseEntity<TermResponse> responsibilityTerm() {
        return ResponseEntity.ok(termAcceptanceService.responsibilityTerm());
    }

    @GetMapping("/responsibility/acceptance")
    @Operation(summary = "Situação de aceite do usuário atual")
    public ResponseEntity<TermAcceptanceStatusResponse> responsibilityStatus() {
        return ResponseEntity.ok(termAcceptanceService.responsibilityStatus());
    }

    @PostMapping("/responsibility/acceptance")
    @Operation(summary = "Registra o aceite do termo de responsabilidade")
    public ResponseEntity<TermAcceptanceStatusResponse> acceptResponsibility(
            @Valid @RequestBody AcceptTermRequest request
    ) {
        return ResponseEntity.ok(termAcceptanceService.acceptResponsibility(request));
    }

    @GetMapping("/health-use")
    @Operation(summary = "Texto e versão do termo de uso na vertical de saúde")
    public ResponseEntity<TermResponse> healthUseTerm() {
        return ResponseEntity.ok(termAcceptanceService.healthUseTerm());
    }

    @GetMapping("/health-use/acceptance")
    @Operation(summary = "Situação de aceite do termo de uso em saúde pelo usuário atual")
    public ResponseEntity<TermAcceptanceStatusResponse> healthUseStatus() {
        return ResponseEntity.ok(termAcceptanceService.healthUseStatus());
    }

    @PostMapping("/health-use/acceptance")
    @Operation(summary = "Registra o aceite do termo de uso em saúde")
    public ResponseEntity<TermAcceptanceStatusResponse> acceptHealthUse(
            @Valid @RequestBody AcceptTermRequest request
    ) {
        return ResponseEntity.ok(termAcceptanceService.acceptHealthUse(request));
    }
}
