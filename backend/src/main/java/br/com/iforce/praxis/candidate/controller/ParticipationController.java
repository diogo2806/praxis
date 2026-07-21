package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.ExtendCandidateLinkRequest;
import br.com.iforce.praxis.candidate.dto.ParticipationMonitoringPageResponse;
import br.com.iforce.praxis.candidate.service.CompanyCandidateLinkService;
import br.com.iforce.praxis.candidate.service.ParticipationMonitoringQueryService;
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

import java.util.Locale;

@RestController
@RequestMapping("/api/v1/candidate-links/participations")
@Tag(name = "Participations", description = "Central unificada de participações individuais e por jornada.")
public class ParticipationController {

    private final ParticipationMonitoringQueryService participationQueryService;
    private final CompanyCandidateLinkService candidateLinkService;
    private final AssessmentJourneyAttemptLifecycleService journeyLifecycleService;

    public ParticipationController(
            ParticipationMonitoringQueryService participationQueryService,
            CompanyCandidateLinkService candidateLinkService,
            AssessmentJourneyAttemptLifecycleService journeyLifecycleService
    ) {
        this.participationQueryService = participationQueryService;
        this.candidateLinkService = candidateLinkService;
        this.journeyLifecycleService = journeyLifecycleService;
    }

    @GetMapping
    @Operation(summary = "Pesquisa participações individuais e por jornada")
    public ResponseEntity<ParticipationMonitoringPageResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String simulationId,
            @RequestParam(required = false) String candidate
    ) {
        return ResponseEntity.ok(
                participationQueryService.search(page, size, simulationId, candidate)
        );
    }

    @PostMapping("/{type}/{participationId}/resend")
    @Operation(summary = "Reenvia o convite de uma participação existente")
    public ResponseEntity<Void> resend(
            @PathVariable String type,
            @PathVariable String participationId
    ) {
        if (isJourney(type)) {
            journeyLifecycleService.resendInvitation(participationId);
        } else if (isIndividual(type)) {
            candidateLinkService.resendExisting(participationId);
        } else {
            throw invalidType();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{type}/{participationId}/extend")
    @Operation(summary = "Amplia ou reativa a validade de uma participação")
    public ResponseEntity<Void> extend(
            @PathVariable String type,
            @PathVariable String participationId,
            @Valid @RequestBody ExtendCandidateLinkRequest request
    ) {
        if (isJourney(type)) {
            journeyLifecycleService.extendValidity(participationId, request.additionalDays());
        } else if (isIndividual(type)) {
            candidateLinkService.extendValidity(participationId, request.additionalDays());
        } else {
            throw invalidType();
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{type}/{participationId}/cancel")
    @Operation(summary = "Cancela uma participação por jornada")
    public ResponseEntity<Void> cancel(
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

    private boolean isJourney(String type) {
        return "journey".equals(normalize(type));
    }

    private boolean isIndividual(String type) {
        return "individual".equals(normalize(type));
    }

    private String normalize(String type) {
        return type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException invalidType() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Tipo de participação inválido. Use individual ou journey."
        );
    }
}
