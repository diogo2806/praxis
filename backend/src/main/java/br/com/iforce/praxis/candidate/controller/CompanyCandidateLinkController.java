package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/candidate-links")
@Tag(name = "Company Candidate Links", description = "Geração de links de simulação para envio direto ao candidato pela empresa.")
public class CompanyCandidateLinkController {

    private final CandidateAttemptService candidateAttemptService;

    public CompanyCandidateLinkController(CandidateAttemptService candidateAttemptService) {
        this.candidateAttemptService = candidateAttemptService;
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
}
