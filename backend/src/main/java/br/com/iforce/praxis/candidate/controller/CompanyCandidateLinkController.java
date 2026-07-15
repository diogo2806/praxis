package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.CandidateAttemptMonitoringResponse;
import br.com.iforce.praxis.candidate.dto.CandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.CreateDirectCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.EvidenceReport;
import br.com.iforce.praxis.candidate.dto.RegisterDispositionRequest;
import br.com.iforce.praxis.candidate.service.CandidateDispositionService;
import br.com.iforce.praxis.candidate.service.CompanyCandidateLinkService;
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

/**
 * Porta de entrada (API) para a empresa gerar e acompanhar links de avaliação.
 *
 * <p>Na visão do processo, é por aqui que o recrutador da empresa: cria o
 * link de simulação para enviar ao candidato, acompanha quem está fazendo a
 * prova agora, registra a decisão final (humana) sobre cada candidato e
 * consulta o relatório de transparência que explica como a pontuação foi
 * calculada. A pontuação é apoio à decisão — a palavra final é sempre de uma
 * pessoa.</p>
 */
@RestController
@RequestMapping("/api/v1/candidate-links")
@Tag(name = "Company Candidate Links", description = "Geração de links de simulação para envio direto ao candidato pela empresa.")
public class CompanyCandidateLinkController {

    private final CandidateAttemptService candidateAttemptService;
    private final CompanyCandidateLinkService companyCandidateLinkService;
    private final CandidateDispositionService candidateDispositionService;
    private final EvidenceReportService evidenceReportService;

    public CompanyCandidateLinkController(
            CandidateAttemptService candidateAttemptService,
            CompanyCandidateLinkService companyCandidateLinkService,
            CandidateDispositionService candidateDispositionService,
            EvidenceReportService evidenceReportService
    ) {
        this.candidateAttemptService = candidateAttemptService;
        this.companyCandidateLinkService = companyCandidateLinkService;
        this.candidateDispositionService = candidateDispositionService;
        this.evidenceReportService = evidenceReportService;
    }

    /** Lista os links de avaliação já gerados pela empresa. */
    @GetMapping
    @Operation(
            summary = "Lista links de candidatos",
            description = "Retorna as tentativas da empresa com URL pública para compartilhamento."
    )
    public ResponseEntity<List<CandidateLinkResponse>> listCandidateLinks(
            @RequestParam(name = "blind", defaultValue = "false") boolean blind
    ) {
        return ResponseEntity.ok(candidateAttemptService.listCompanyLinks(blind));
    }

    /** Lista as provas em monitoramento operacional. */
    @GetMapping("/live-attempts")
    @Operation(
            summary = "Lista tentativas monitoradas",
            description = "Retorna tentativas ativas, pausadas ou concluídas com progresso operacional para a tela de monitoramento."
    )
    public ResponseEntity<List<CandidateAttemptMonitoringResponse>> listLiveAttempts() {
        return ResponseEntity.ok(candidateAttemptService.listLiveAttempts());
    }

    /**
     * Cria uma nova tentativa. O applicationCycleId torna reenvios técnicos da mesma criação
     * idempotentes, enquanto um novo ciclo permite reaplicar a mesma avaliação à mesma pessoa.
     */
    @PostMapping
    @Operation(
            summary = "Cria uma nova tentativa e gera o link",
            description = "Cria uma nova aplicação. O mesmo applicationCycleId reaproveita somente a criação equivalente; um novo ciclo cria outra tentativa."
    )
    public ResponseEntity<CreateCandidateLinkResponse> createCandidateLink(
            @Valid @RequestBody CreateDirectCandidateLinkRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(companyCandidateLinkService.createNewAttempt(request));
    }

    /**
     * Retorna novamente o link de uma tentativa existente, sem criar nova tentativa e sem consumir
     * um novo crédito. A tentativa precisa pertencer à empresa autenticada.
     */
    @PostMapping("/{attemptId}/resend")
    @Operation(
            summary = "Reenvia um link existente",
            description = "Retorna o mesmo link da tentativa informada, sem criar uma nova aplicação."
    )
    public ResponseEntity<CreateCandidateLinkResponse> resendCandidateLink(@PathVariable String attemptId) {
        return ResponseEntity.ok(companyCandidateLinkService.resendExistingLink(attemptId));
    }

    /** Registra a decisão humana final sobre o candidato. */
    @PostMapping("/{attemptId}/disposition")
    @Operation(
            summary = "Registra a decisão humana sobre o candidato",
            description = "Registra na trilha append-only quem decidiu, quando e por quê. A pontuação é apenas apoio: a decisão final cabe a uma pessoa."
    )
    public ResponseEntity<Void> registerDisposition(
            @PathVariable String attemptId,
            @Valid @RequestBody RegisterDispositionRequest request
    ) {
        candidateDispositionService.register(attemptId, request);
        return ResponseEntity.noContent().build();
    }

    /** Monta o relatório de transparência da avaliação de um candidato. */
    @GetMapping("/{attemptId}/evidence-report")
    @Operation(
            summary = "Relatório de transparência do scoring",
            description = "Documento consolidado: declaração de scoring determinístico (sem IA, sem dados de treino), fórmula e versão do blueprint, caminho do candidato, pontos por competência, trilha append-only e a decisão humana."
    )
    public ResponseEntity<EvidenceReport> evidenceReport(@PathVariable String attemptId) {
        return ResponseEntity.ok(evidenceReportService.build(attemptId));
    }
}
