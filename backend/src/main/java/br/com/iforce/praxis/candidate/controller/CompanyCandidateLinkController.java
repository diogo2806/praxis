package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.CandidateAttemptMonitoringResponse;
import br.com.iforce.praxis.candidate.dto.CandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.EvidenceReport;
import br.com.iforce.praxis.candidate.dto.RegisterDispositionRequest;
import br.com.iforce.praxis.candidate.service.CandidateDispositionService;
import br.com.iforce.praxis.candidate.service.EvidenceReportService;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/candidate-links")
@Tag(name = "Company Candidate Links", description = "Geração de links de simulação para envio direto ao candidato pela empresa.")
public class CompanyCandidateLinkController {

    private final CandidateAttemptService candidateAttemptService;
    private final CandidateDispositionService candidateDispositionService;
    private final EvidenceReportService evidenceReportService;

    public CompanyCandidateLinkController(
            CandidateAttemptService candidateAttemptService,
            CandidateDispositionService candidateDispositionService,
            EvidenceReportService evidenceReportService
    ) {
        this.candidateAttemptService = candidateAttemptService;
        this.candidateDispositionService = candidateDispositionService;
        this.evidenceReportService = evidenceReportService;
    }

    @GetMapping
    @Operation(
            summary = "Lista links de candidatos",
            description = "Retorna as tentativas da empresa com URL publica para compartilhamento."
    )
    public ResponseEntity<List<CandidateLinkResponse>> listCandidateLinks(
            @RequestParam(name = "blind", defaultValue = "false") boolean blind
    ) {
        return ResponseEntity.ok(candidateAttemptService.listCompanyLinks(blind));
    }

    @GetMapping("/live-attempts")
    @Operation(
            summary = "Lista tentativas em andamento",
            description = "Retorna tentativas ativas ou pausadas com progresso operacional para a tela de monitoramento."
    )
    public ResponseEntity<List<CandidateAttemptMonitoringResponse>> listLiveAttempts() {
        return ResponseEntity.ok(candidateAttemptService.listLiveAttempts());
    }

    @PostMapping
    @Operation(
            summary = "Gera link para candidato",
            description = "Cria uma tentativa de simulação e retorna o link para envio ao candidato por email ou WhatsApp."
    )
    public ResponseEntity<CreateCandidateLinkResponse> createCandidateLink(
            @Valid @RequestBody CreateCandidateLinkRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(candidateAttemptService.createCompanyLink(request));
    }

    @PostMapping("/{attemptId}/disposition")
    @Operation(
            summary = "Registra a decisão humana sobre o candidato",
            description = "Registra na trilha append-only quem decidiu, quando e por quê. O score é "
                    + "apenas apoio: a decisão final é sempre de uma pessoa."
    )
    public ResponseEntity<Void> registerDisposition(
            @PathVariable String attemptId,
            @Valid @RequestBody RegisterDispositionRequest request
    ) {
        candidateDispositionService.register(attemptId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{attemptId}/evidence-report")
    @Operation(
            summary = "Relatório de transparência do scoring",
            description = "Documento consolidado: declaração de scoring determinístico (sem IA, sem "
                    + "dados de treino), fórmula e versão do blueprint, caminho do candidato, pontos "
                    + "por competência, trilha imutável e a decisão humana."
    )
    public ResponseEntity<EvidenceReport> evidenceReport(@PathVariable String attemptId) {
        return ResponseEntity.ok(evidenceReportService.build(attemptId));
    }
}
