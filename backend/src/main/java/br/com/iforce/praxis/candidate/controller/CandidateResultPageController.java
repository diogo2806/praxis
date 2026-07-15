package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.CandidateResultPageResponse;
import br.com.iforce.praxis.candidate.service.CandidateResultPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/candidate/results")
@Tag(name = "Resultado público do candidato", description = "Dados seguros para a página final da pessoa candidata.")
public class CandidateResultPageController {

    private final CandidateResultPageService candidateResultPageService;

    public CandidateResultPageController(CandidateResultPageService candidateResultPageService) {
        this.candidateResultPageService = candidateResultPageService;
    }

    @GetMapping("/{token}")
    @Operation(
            summary = "Carrega página final do candidato",
            description = "Retorna somente avaliação, estado final e URL de retorno ao ATS, sem pontuação ou respostas."
    )
    public ResponseEntity<CandidateResultPageResponse> getCandidateResultPage(@PathVariable String token) {
        return ResponseEntity.ok(candidateResultPageService.findByToken(token));
    }
}
