package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.DataSubjectRequest;
import br.com.iforce.praxis.candidate.dto.HealthConsentRequest;
import br.com.iforce.praxis.candidate.dto.HealthConsentStatusResponse;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaRequest;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.candidate.dto.ReviewRequest;
import br.com.iforce.praxis.candidate.service.CandidateDataRequestService;
import br.com.iforce.praxis.candidate.service.CandidateHealthConsentService;
import br.com.iforce.praxis.candidate.service.CandidateReviewRequestService;
import br.com.iforce.praxis.candidate.service.PublicCandidateFlowSecurity;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.integrity.service.CandidateIntegrityService;
import br.com.iforce.praxis.shared.privacy.service.CandidatePrivacyNoticeService;
import br.com.iforce.praxis.shared.privacy.service.CandidatePrivacyNoticeService.CandidatePrivacyNoticeAcknowledgementRequest;
import br.com.iforce.praxis.shared.privacy.service.CandidatePrivacyNoticeService.CandidatePrivacyNoticeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/candidate/attempts")
@Tag(name = "Participacoes", description = "Fluxo publico do candidato para executar a avaliacao.")
public class CandidateAttemptController {

    private static final String INTEGRITY_SESSION_HEADER = "X-Praxis-Integrity-Session";

    private final CandidateAttemptService candidateAttemptService;
    private final CandidateReviewRequestService candidateReviewRequestService;
    private final CandidateDataRequestService candidateDataRequestService;
    private final CandidateHealthConsentService candidateHealthConsentService;
    private final PublicCandidateFlowSecurity publicCandidateFlowSecurity;
    private final CandidatePrivacyNoticeService candidatePrivacyNoticeService;
    private final CandidateIntegrityService candidateIntegrityService;

    public CandidateAttemptController(
            CandidateAttemptService candidateAttemptService,
            CandidateReviewRequestService candidateReviewRequestService,
            CandidateDataRequestService candidateDataRequestService,
            CandidateHealthConsentService candidateHealthConsentService,
            PublicCandidateFlowSecurity publicCandidateFlowSecurity,
            CandidatePrivacyNoticeService candidatePrivacyNoticeService,
            CandidateIntegrityService candidateIntegrityService
    ) {
        this.candidateAttemptService = candidateAttemptService;
        this.candidateReviewRequestService = candidateReviewRequestService;
        this.candidateDataRequestService = candidateDataRequestService;
        this.candidateHealthConsentService = candidateHealthConsentService;
        this.publicCandidateFlowSecurity = publicCandidateFlowSecurity;
        this.candidatePrivacyNoticeService = candidatePrivacyNoticeService;
        this.candidateIntegrityService = candidateIntegrityService;
    }

    @GetMapping("/{attemptToken}/privacy-notice")
    @Operation(summary = "Retorna os documentos legais aplicáveis à participação")
    public ResponseEntity<CandidatePrivacyNoticeResponse> getPrivacyNotice(@PathVariable String attemptToken) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        return ResponseEntity.ok(candidatePrivacyNoticeService.getNotice(attemptToken));
    }

    @PostMapping("/{attemptToken}/privacy-notice/acknowledgement")
    @Operation(summary = "Registra o aceite versionado dos Termos de Uso e a ciência do aviso de privacidade")
    public ResponseEntity<Void> acknowledgePrivacyNotice(
            @PathVariable String attemptToken,
            @Valid @RequestBody CandidatePrivacyNoticeAcknowledgementRequest request
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidatePrivacyNoticeService.acknowledge(attemptToken, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{attemptToken}/health-consent")
    @Operation(summary = "Retorna o estado mínimo do consentimento específico de saúde")
    public ResponseEntity<HealthConsentStatusResponse> getHealthConsentStatus(@PathVariable String attemptToken) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        return ResponseEntity.ok(candidateHealthConsentService.getStatus(attemptToken));
    }

    @GetMapping("/{attemptToken}")
    @Operation(
            summary = "Carrega participacao do candidato",
            description = "Retorna somente a etapa atual para uma sessão técnica ativa, com os aceites legais aplicáveis registrados e sem regras futuras."
    )
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public ResponseEntity<ParticipacaoResponse> getCandidateAttempt(
            @PathVariable String attemptToken,
            @RequestHeader(value = INTEGRITY_SESSION_HEADER, required = false) String integritySessionId
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidatePrivacyNoticeService.assertAcknowledged(attemptToken);
        candidateHealthConsentService.assertConsentGranted(attemptToken);
        candidateIntegrityService.requireActiveSession(attemptToken, integritySessionId);
        ParticipacaoResponse response = candidateAttemptService.findCandidateAttempt(attemptToken);
        return ResponseEntity.ok(publicCandidateFlowSecurity.sanitize(attemptToken, response));
    }

    @PostMapping("/{attemptToken}/answers")
    @Operation(
            summary = "Registra resposta do candidato",
            description = "Registra a resposta somente com os aceites legais aplicáveis e a sessão técnica ativos."
    )
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public ResponseEntity<RegistrarRespostaResponse> submitAnswer(
            @PathVariable String attemptToken,
            @RequestHeader(value = INTEGRITY_SESSION_HEADER, required = false) String integritySessionId,
            @Valid @RequestBody RegistrarRespostaRequest request
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidatePrivacyNoticeService.assertAcknowledged(attemptToken);
        candidateHealthConsentService.assertConsentGranted(attemptToken);
        candidateIntegrityService.requireActiveSession(attemptToken, integritySessionId);
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
    @Operation(summary = "Registra de forma idempotente o consentimento específico de saúde")
    public ResponseEntity<Void> registerHealthConsent(
            @PathVariable String attemptToken,
            @Valid @RequestBody HealthConsentRequest request
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidateHealthConsentService.register(attemptToken, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{attemptToken}/health-consent")
    @Operation(summary = "Revoga o consentimento específico de saúde")
    public ResponseEntity<Void> revokeHealthConsent(@PathVariable String attemptToken) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidateHealthConsentService.revoke(attemptToken);
        return ResponseEntity.noContent().build();
    }
}
