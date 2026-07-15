package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.DataSubjectRequest;
import br.com.iforce.praxis.candidate.dto.HealthConsentRequest;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaRequest;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.candidate.dto.ReviewRequest;
import br.com.iforce.praxis.candidate.service.CandidateDataRequestService;
import br.com.iforce.praxis.candidate.service.CandidateHealthConsentService;
import br.com.iforce.praxis.candidate.service.CandidateReviewRequestService;
import br.com.iforce.praxis.candidate.service.PublicCandidateFlowSecurity;
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
    private final CandidateDataRequestService candidateDataRequestService;
    private final CandidateHealthConsentService candidateHealthConsentService;
    private final PublicCandidateFlowSecurity publicCandidateFlowSecurity;

    public CandidateAttemptController(
            CandidateAttemptService candidateAttemptService,
            CandidateReviewRequestService candidateReviewRequestService,
            CandidateDataRequestService candidateDataRequestService,
            CandidateHealthConsentService candidateHealthConsentService,
            PublicCandidateFlowSecurity publicCandidateFlowSecurity
    ) {
        this.candidateAttemptService = candidateAttemptService;
        this.candidateReviewRequestService = candidateReviewRequestService;
        this.candidateDataRequestService = candidateDataRequestService;
        this.candidateHealthConsentService = candidateHealthConsentService;
        this.publicCandidateFlowSecurity = publicCandidateFlowSecurity;
    }

    @GetMapping("/{attemptToken}")
    @Operation(
            summary = "Carrega participacao do candidato",
            description = "Retorna somente a etapa atual, com identificadores opacos e sem regras de navegação futuras."
    )
    public ResponseEntity<ParticipacaoResponse> getCandidateAttempt(@PathVariable String attemptToken) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        ParticipacaoResponse response = candidateAttemptService.findCandidateAttempt(attemptToken);
        return ResponseEntity.ok(publicCandidateFlowSecurity.sanitize(attemptToken, response));
    }

    @PostMapping("/{attemptToken}/answers")
    @Operation(
            summary = "Registra resposta do candidato",
            description = "Registra a resposta da etapa atual sem aceitar identificadores internos enviados pelo cliente."
    )
    public ResponseEntity<RegistrarRespostaResponse> submitAnswer(
            @PathVariable String attemptToken,
            @Valid @RequestBody RegistrarRespostaRequest request
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        RegistrarRespostaResponse response = candidateAttemptService.submitAnswer(
                attemptToken,
                publicCandidateFlowSecurity.sanitizeRequest(request)
        );
        return ResponseEntity.ok(publicCandidateFlowSecurity.sanitize(attemptToken, response));
    }

    @PostMapping("/{attemptToken}/review-request")
    public ResponseEntity<Void> requestHumanReview(
            @PathVariable String attemptToken,
            @Valid @RequestBody(required = false) ReviewRequest request
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidateReviewRequestService.register(attemptToken, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{attemptToken}/data-request")
    public ResponseEntity<Void> requestDataSubjectRight(
            @PathVariable String attemptToken,
            @Valid @RequestBody DataSubjectRequest request
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidateDataRequestService.register(attemptToken, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{attemptToken}/health-consent")
    public ResponseEntity<Void> registerHealthConsent(
            @PathVariable String attemptToken,
            @Valid @RequestBody HealthConsentRequest request
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidateHealthConsentService.register(attemptToken, request);
        return ResponseEntity.noContent().build();
    }
}
