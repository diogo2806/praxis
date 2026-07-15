package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.CandidateAttemptMonitoringResponse;
import br.com.iforce.praxis.candidate.dto.CandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
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

    /**
     * Lista os links de avaliação já gerados pela empresa.
     *
     * @param blind quando verdadeiro, oculta dados que identificam o
     *              candidato (avaliação às cegas, para reduzir viés)
     * @return os links/tentativas da empresa, com a URL pública de cada um
     */
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

    /**
     * Lista as provas em monitoramento operacional.
     *
     * <p>Alimenta a tela de monitoramento, mostrando progresso de tentativas
     * em andamento, pausadas e concluídas.</p>
     *
     * @return as tentativas monitoradas com seu progresso
     */
    @GetMapping("/live-attempts")
    @Operation(
            summary = "Lista tentativas monitoradas",
            description = "Retorna tentativas ativas, pausadas ou concluídas com progresso operacional para a tela de monitoramento."
    )
    public ResponseEntity<List<CandidateAttemptMonitoringResponse>> listLiveAttempts() {
        return ResponseEntity.ok(candidateAttemptService.listLiveAttempts());
    }

    /**
     * Cria uma nova aplicação de avaliação para um candidato.
     *
     * <p>O {@code applicationCycleId} identifica a vaga/processo/etapa. Uma repetição
     * do mesmo pedido é idempotente; outro ciclo cria uma tentativa independente.</p>
     *
     * @param request dados do candidato, avaliação e ciclo da nova aplicação
     * @return o link criado ou reaproveitado por repetição idempotente
     */
    @PostMapping
    @Operation(
            summary = "Cria nova aplicação para candidato",
            description = "Cria uma tentativa independente por applicationCycleId. Repetir o mesmo ciclo apenas reconcilia a mesma requisição; para reenviar um link existente use POST /{attemptId}/resend."
    )
    public ResponseEntity<CreateCandidateLinkResponse> createCandidateLink(
            @Valid @RequestBody CreateCandidateLinkRequest request
    ) {
        CreateCandidateLinkResponse response = companyCandidateLinkService.createNewApplication(request);
        HttpStatus status = response.reused() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Reenvia um link existente sem criar nova tentativa.
     *
     * @param attemptId tentativa que pertence à empresa autenticada
     * @return o mesmo link público já associado à tentativa
     */
    @PostMapping("/{attemptId}/resend")
    @Operation(
            summary = "Reenvia link existente",
            description = "Valida a empresa autenticada e retorna exatamente o link da tentativa informada, sem criar nova aplicação ou consumir crédito adicional."
    )
    public ResponseEntity<CreateCandidateLinkResponse> resendCandidateLink(@PathVariable String attemptId) {
        return ResponseEntity.ok(companyCandidateLinkService.resendExisting(attemptId));
    }

    /**
     * Registra a decisão humana final sobre o candidato.
     *
     * <p>Guarda na trilha de auditoria quem decidiu, quando e por quê (por
     * exemplo, avançar ou reprovar). A pontuação é apenas apoio: a decisão é
     * sempre de uma pessoa.</p>
     *
     * @param attemptId identificador da participação do candidato
     * @param request a decisão tomada e sua justificativa
     * @return confirmação sem conteúdo (apenas registra a decisão)
     */
    @PostMapping("/{attemptId}/disposition")
    @Operation(
            summary = "Registra a decisão humana sobre o candidato",
            description = "Registra na trilha append-only quem decidiu, quando e por quê. A pontuação é "
                    + "apenas apoio: a decisão final cabe a uma pessoa."
    )
    public ResponseEntity<Void> registerDisposition(
            @PathVariable String attemptId,
            @Valid @RequestBody RegisterDispositionRequest request
    ) {
        candidateDispositionService.register(attemptId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Monta o relatório de transparência da avaliação de um candidato.
     *
     * <p>Documento que comprova como a nota foi calculada: declara que a
     * pontuação é determinística (sem IA e sem dados de treino), mostra a
     * fórmula e a versão usada, o caminho que o candidato percorreu, os
     * pontos por competência, a trilha de auditoria e a decisão humana.
     * Serve para auditoria, conformidade e para responder ao candidato.</p>
     *
     * @param attemptId identificador da participação do candidato
     * @return o relatório consolidado de transparência
     */
    @GetMapping("/{attemptId}/evidence-report")
    @Operation(
            summary = "Relatório de transparência do scoring",
            description = "Documento consolidado: declaração de scoring determinístico (sem IA, sem "
                    + "dados de treino), fórmula e versão do blueprint, caminho do candidato, pontos "
                    + "por competência, trilha append-only e a decisão humana."
    )
    public ResponseEntity<EvidenceReport> evidenceReport(@PathVariable String attemptId) {
        return ResponseEntity.ok(evidenceReportService.build(attemptId));
    }
}
