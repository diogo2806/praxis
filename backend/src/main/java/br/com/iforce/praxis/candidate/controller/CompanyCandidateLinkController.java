package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.CandidateAttemptMonitoringPageResponse;
import br.com.iforce.praxis.candidate.dto.CandidateAttemptMonitoringResponse;
import br.com.iforce.praxis.candidate.dto.CandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.CreateCandidateLinkResponse;
import br.com.iforce.praxis.candidate.dto.EvidenceReport;
import br.com.iforce.praxis.candidate.dto.ExtendCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.ParticipationMonitoringPageResponse;
import br.com.iforce.praxis.candidate.dto.RegisterDispositionRequest;
import br.com.iforce.praxis.candidate.service.CandidateAttemptMonitoringQueryService;
import br.com.iforce.praxis.candidate.service.CandidateDispositionService;
import br.com.iforce.praxis.candidate.service.CompanyCandidateLinkService;
import br.com.iforce.praxis.candidate.service.EvidenceReportService;
import br.com.iforce.praxis.candidate.service.LegacyCandidateLinkQueryService;
import br.com.iforce.praxis.candidate.service.ParticipationMonitoringQueryService;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import br.com.iforce.praxis.journey.service.AssessmentJourneyAttemptLifecycleService;
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
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/v1/candidate-links")
@Tag(name = "Company Candidate Links", description = "Geração de links de simulação para envio direto ao candidato pela empresa.")
public class CompanyCandidateLinkController {

    private final CandidateAttemptService candidateAttemptService;
    private final CandidateAttemptMonitoringQueryService monitoringQueryService;
    private final CompanyCandidateLinkService companyCandidateLinkService;
    private final LegacyCandidateLinkQueryService legacyCandidateLinkQueryService;
    private final CandidateDispositionService candidateDispositionService;
    private final EvidenceReportService evidenceReportService;
    private final ParticipationMonitoringQueryService participationQueryService;
    private final AssessmentJourneyAttemptLifecycleService journeyLifecycleService;

    public CompanyCandidateLinkController(
            CandidateAttemptService candidateAttemptService,
            CandidateAttemptMonitoringQueryService monitoringQueryService,
            CompanyCandidateLinkService companyCandidateLinkService,
            LegacyCandidateLinkQueryService legacyCandidateLinkQueryService,
            CandidateDispositionService candidateDispositionService,
            EvidenceReportService evidenceReportService,
            ParticipationMonitoringQueryService participationQueryService,
            AssessmentJourneyAttemptLifecycleService journeyLifecycleService
    ) {
        this.candidateAttemptService = candidateAttemptService;
        this.monitoringQueryService = monitoringQueryService;
        this.companyCandidateLinkService = companyCandidateLinkService;
        this.legacyCandidateLinkQueryService = legacyCandidateLinkQueryService;
        this.candidateDispositionService = candidateDispositionService;
        this.evidenceReportService = evidenceReportService;
        this.participationQueryService = participationQueryService;
        this.journeyLifecycleService = journeyLifecycleService;
    }

    @GetMapping
    @Operation(summary = "Lista links de candidatos (legado)")
    public ResponseEntity<List<CandidateLinkResponse>> listCandidateLinks(
            @RequestParam(name = "blind", defaultValue = "false") boolean blind
    ) {
        return ResponseEntity.ok(legacyCandidateLinkQueryService.listAll(blind));
    }

    @GetMapping("/live-attempts")
    @Operation(summary = "Lista tentativas monitoradas (legado)")
    public ResponseEntity<List<CandidateAttemptMonitoringResponse>> listLiveAttempts() {
        return ResponseEntity.ok(candidateAttemptService.listLiveAttempts());
    }

    @GetMapping("/attempts")
    @Operation(summary = "Pesquisa tentativas para o centro operacional")
    public ResponseEntity<CandidateAttemptMonitoringPageResponse> searchAttempts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String simulationId,
            @RequestParam(required = false) String candidate
    ) {
        return ResponseEntity.ok(
                monitoringQueryService.search(page, size, status, simulationId, candidate)
        );
    }

    @GetMapping("/participations")
    @Operation(summary = "Pesquisa participações individuais e por jornada")
    public ResponseEntity<ParticipationMonitoringPageResponse> searchParticipations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String simulationId,
            @RequestParam(required = false) String candidate
    ) {
        return ResponseEntity.ok(
                participationQueryService.search(page, size, simulationId, candidate)
        );
    }

    @PostMapping
    @Operation(summary = "Cria nova aplicação para candidato")
    public ResponseEntity<CreateCandidateLinkResponse> createCandidateLink(
            @Valid @RequestBody CreateCandidateLinkRequest request
    ) {
        CreateCandidateLinkResponse response = companyCandidateLinkService.createNewApplication(request);
        HttpStatus status = response.reused() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    @PostMapping("/{attemptId}/resend")
    @Operation(
            summary = "Reenvia link vigente",
            description = "Links expirados devem ser reativados antes pelo endpoint de extensão."
    )
    public ResponseEntity<CreateCandidateLinkResponse> resendCandidateLink(@PathVariable String attemptId) {
        return ResponseEntity.ok(companyCandidateLinkService.resendExisting(attemptId));
    }

    @PostMapping("/{attemptId}/extend")
    @Operation(
            summary = "Adiciona dias à validade do link",
            description = "Soma dias ao vencimento atual ou reativa a partir de agora quando o link já expirou."
    )
    public ResponseEntity<CreateCandidateLinkResponse> extendCandidateLink(
            @PathVariable String attemptId,
            @Valid @RequestBody ExtendCandidateLinkRequest request
    ) {
        return ResponseEntity.ok(
                companyCandidateLinkService.extendValidity(attemptId, request.additionalDays())
        );
    }

    @PostMapping("/participations/{type}/{participationId}/resend")
    @Operation(summary = "Reenvia o convite de uma participação existente")
    public ResponseEntity<Void> resendParticipation(
            @PathVariable String type,
            @PathVariable String participationId
    ) {
        if (isJourney(type)) {
            journeyLifecycleService.resendInvitation(participationId);
        } else if (isIndividual(type)) {
            companyCandidateLinkService.resendExisting(participationId);
        } else {
            throw invalidParticipationType();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/participations/{type}/{participationId}/extend")
    @Operation(summary = "Amplia ou reativa a validade de uma participação")
    public ResponseEntity<Void> extendParticipation(
            @PathVariable String type,
            @PathVariable String participationId,
            @Valid @RequestBody ExtendCandidateLinkRequest request
    ) {
        if (isJourney(type)) {
            journeyLifecycleService.extendValidity(participationId, request.additionalDays());
        } else if (isIndividual(type)) {
            companyCandidateLinkService.extendValidity(participationId, request.additionalDays());
        } else {
            throw invalidParticipationType();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/participations/{type}/{participationId}/cancel")
    @Operation(summary = "Cancela uma participação por jornada")
    public ResponseEntity<Void> cancelParticipation(
            @PathVariable String type,
            @PathVariable String participationId
    ) {
        if (!isJourney(type)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Somente participações por jornada podem ser canceladas por este endpoint."
            );
        }
        journeyLifecycleService.cancel(participationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{attemptId}/disposition")
    @Operation(summary = "Registra a decisão humana sobre o candidato")
    public ResponseEntity<Void> registerDisposition(
            @PathVariable String attemptId,
            @Valid @RequestBody RegisterDispositionRequest request
    ) {
        candidateDispositionService.register(attemptId, request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{attemptId}/evidence-report")
    @Operation(summary = "Relatório de transparência do scoring")
    public ResponseEntity<EvidenceReport> evidenceReport(@PathVariable String attemptId) {
        return ResponseEntity.ok(evidenceReportService.build(attemptId));
    }

    private boolean isJourney(String type) {
        return "journey".equals(normalize(type));
    }

    private boolean isIndividual(String type) {
        return "individual".equals(normalize(type));
    }

    private String normalize(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidParticipationType() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tipo de participação inválido. Use individual ou journey."
        );
    }
}
