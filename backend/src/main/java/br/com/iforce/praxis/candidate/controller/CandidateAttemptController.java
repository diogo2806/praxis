package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.CandidateAttemptResponse;
import br.com.iforce.praxis.candidate.dto.SubmitAnswerRequest;
import br.com.iforce.praxis.candidate.dto.SubmitAnswerResponse;
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
@Tag(name = "Candidate Attempts", description = "Fluxo público do candidato para executar a simulação situacional.")
public class CandidateAttemptController {

    private final CandidateAttemptService candidateAttemptService;

    public CandidateAttemptController(CandidateAttemptService candidateAttemptService) {
        this.candidateAttemptService = candidateAttemptService;
    }

    @GetMapping("/{attemptId}")
    @Operation(
            summary = "Carrega tentativa do candidato",
            description = "Retorna o turno atual sem expor gabarito, pesos, marcadores críticos ou regras internas."
    )
    public ResponseEntity<CandidateAttemptResponse> getCandidateAttempt(@PathVariable String attemptId) {
        return ResponseEntity.ok(candidateAttemptService.findCandidateAttempt(attemptId));
    }

    @PostMapping("/{attemptId}/answers")
    @Operation(
            summary = "Salva resposta do candidato",
            description = "Persiste resposta de forma idempotente por attemptId e nodeId, recalculando o resultado ao finalizar."
    )
    public ResponseEntity<SubmitAnswerResponse> submitAnswer(
            @PathVariable String attemptId,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        return ResponseEntity.ok(candidateAttemptService.submitAnswer(attemptId, request));
    }
}
