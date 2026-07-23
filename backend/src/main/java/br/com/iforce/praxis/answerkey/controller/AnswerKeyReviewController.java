package br.com.iforce.praxis.answerkey.controller;

import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.AssignmentRequest;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.CreateRoundRequest;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.EvidenceRequest;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.OptionReviewRequest;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.ReviewSummaryResponse;
import br.com.iforce.praxis.answerkey.dto.AnswerKeyReviewDtos.RoundResponse;
import br.com.iforce.praxis.answerkey.service.AnswerKeyReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulations/{simulationId}/versions/{versionNumber}/answer-key-review")
@Tag(name = "Answer Key Review", description = "Construção, revisão e aprovação científica do gabarito.")
public class AnswerKeyReviewController {

    private final AnswerKeyReviewService answerKeyReviewService;

    public AnswerKeyReviewController(AnswerKeyReviewService answerKeyReviewService) {
        this.answerKeyReviewService = answerKeyReviewService;
    }

    @PostMapping("/rounds")
    @Operation(summary = "Cria uma rodada de revisão do gabarito")
    public ResponseEntity<RoundResponse> createRound(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @Valid @RequestBody CreateRoundRequest request
    ) {
        return ResponseEntity.status(201).body(answerKeyReviewService.createRound(simulationId, versionNumber, request));
    }

    @GetMapping("/rounds/latest")
    @Operation(summary = "Consulta a rodada mais recente e seus indicadores")
    public ResponseEntity<ReviewSummaryResponse> getLatestSummary(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return ResponseEntity.ok(answerKeyReviewService.getLatestSummary(simulationId, versionNumber));
    }

    @GetMapping("/rounds/{roundId}")
    @Operation(summary = "Consulta uma rodada e seus indicadores")
    public ResponseEntity<ReviewSummaryResponse> getSummary(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable UUID roundId
    ) {
        return ResponseEntity.ok(answerKeyReviewService.getSummary(simulationId, versionNumber, roundId));
    }

    @PostMapping("/rounds/{roundId}/assignments")
    @Operation(summary = "Convida especialista ou designa aprovador")
    public ResponseEntity<ReviewSummaryResponse> invite(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable UUID roundId,
            @Valid @RequestBody AssignmentRequest request
    ) {
        return ResponseEntity.ok(answerKeyReviewService.invite(simulationId, versionNumber, roundId, request));
    }

    @PutMapping("/rounds/{roundId}/evidence/{nodeId}")
    @Operation(summary = "Vincula cenário, tarefa, risco, competência e indicador")
    public ResponseEntity<ReviewSummaryResponse> saveEvidence(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable UUID roundId,
            @PathVariable String nodeId,
            @Valid @RequestBody EvidenceRequest request
    ) {
        return ResponseEntity.ok(answerKeyReviewService.saveEvidence(
                simulationId,
                versionNumber,
                roundId,
                nodeId,
                request
        ));
    }

    @PutMapping("/rounds/{roundId}/options/{nodeId}/{optionId}/reviews/me")
    @Operation(summary = "Registra a avaliação do especialista para uma alternativa")
    public ResponseEntity<ReviewSummaryResponse> reviewOption(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable UUID roundId,
            @PathVariable String nodeId,
            @PathVariable String optionId,
            @Valid @RequestBody OptionReviewRequest request
    ) {
        return ResponseEntity.ok(answerKeyReviewService.reviewOption(
                simulationId,
                versionNumber,
                roundId,
                nodeId,
                optionId,
                request
        ));
    }

    @PostMapping("/rounds/{roundId}/submit")
    @Operation(summary = "Conclui a revisão do especialista atual")
    public ResponseEntity<ReviewSummaryResponse> submitExpertReview(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable UUID roundId
    ) {
        return ResponseEntity.ok(answerKeyReviewService.submitExpertReview(simulationId, versionNumber, roundId));
    }

    @PostMapping("/rounds/{roundId}/approve")
    @Operation(summary = "Aprova o gabarito após atingir os critérios mínimos")
    public ResponseEntity<ReviewSummaryResponse> approve(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable UUID roundId
    ) {
        return ResponseEntity.ok(answerKeyReviewService.approve(simulationId, versionNumber, roundId));
    }

    @GetMapping(value = "/rounds/{roundId}/report.csv", produces = "text/csv")
    @Operation(summary = "Exporta matriz de evidências, consenso e histórico de aprovação")
    public ResponseEntity<byte[]> exportTechnicalReport(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @PathVariable UUID roundId
    ) {
        byte[] report = answerKeyReviewService.exportTechnicalReport(simulationId, versionNumber, roundId);
        String filename = "gabarito-" + simulationId + "-v" + versionNumber + "-" + roundId + ".csv";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(report);
    }
}
