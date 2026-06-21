package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaRequest;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.candidate.dto.ReviewRequest;
import br.com.iforce.praxis.candidate.service.CandidateReviewRequestService;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
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

@RestController
@RequestMapping("/candidate/attempts")
@Tag(name = "Participacoes", description = "Fluxo publico do candidato para executar a avaliacao.")
public class CandidateAttemptController {

    private final CandidateAttemptService candidateAttemptService;
    private final CandidateReviewRequestService candidateReviewRequestService;

    public CandidateAttemptController(
            CandidateAttemptService candidateAttemptService,
            CandidateReviewRequestService candidateReviewRequestService
    ) {
        this.candidateAttemptService = candidateAttemptService;
        this.candidateReviewRequestService = candidateReviewRequestService;
    }

    @GetMapping("/{attemptId}")
    @Operation(
            summary = "Carrega participacao do candidato",
            description = "Retorna a etapa atual sem expor gabarito, pesos, identificadores internos ou regras tecnicas."
    )
    public ResponseEntity<ParticipacaoResponse> getCandidateAttempt(@PathVariable String attemptId) {
        return ResponseEntity.ok(candidateAttemptService.findCandidateAttempt(attemptId));
    }

    @PostMapping("/{attemptId}/answers")
    @Operation(
            summary = "Registra resposta do candidato",
            description = "Registra a resposta da etapa atual e recalcula o resultado quando a avaliacao termina."
    )
    public ResponseEntity<RegistrarRespostaResponse> submitAnswer(
            @PathVariable String attemptId,
            @Valid @RequestBody RegistrarRespostaRequest request
    ) {
        return ResponseEntity.ok(candidateAttemptService.submitAnswer(attemptId, request));
    }

    @PostMapping("/{attemptId}/review-request")
    @Operation(
            summary = "Solicita revisão humana",
            description = "Registra o pedido de revisão humana do candidato (LGPD art. 20). Uma pessoa "
                    + "decide; este pedido fica na trilha imutável para o recrutador."
    )
    public ResponseEntity<Void> requestHumanReview(
            @PathVariable String attemptId,
            @Valid @RequestBody(required = false) ReviewRequest request
    ) {
        candidateReviewRequestService.register(attemptId, request);
        return ResponseEntity.noContent().build();
    }
}
