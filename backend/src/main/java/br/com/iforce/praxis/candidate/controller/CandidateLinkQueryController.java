package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.CandidateLinkPageResponse;
import br.com.iforce.praxis.candidate.service.CandidateLinkQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candidate-links")
@Tag(name = "Company Candidate Links", description = "Consulta paginada de links e participações da empresa.")
public class CandidateLinkQueryController {

    private final CandidateLinkQueryService candidateLinkQueryService;

    public CandidateLinkQueryController(CandidateLinkQueryService candidateLinkQueryService) {
        this.candidateLinkQueryService = candidateLinkQueryService;
    }

    @GetMapping("/page")
    @Operation(
            summary = "Pesquisa links de candidatos",
            description = "Retorna uma página filtrável por status, avaliação, versão e candidato, sem corte silencioso."
    )
    public ResponseEntity<CandidateLinkPageResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(defaultValue = "false") boolean blind,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String simulationId,
            @RequestParam(required = false) Integer versionNumber,
            @RequestParam(required = false) String candidate
    ) {
        return ResponseEntity.ok(candidateLinkQueryService.search(
                page,
                size,
                blind,
                status,
                simulationId,
                versionNumber,
                candidate
        ));
    }
}
