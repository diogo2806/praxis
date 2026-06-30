package br.com.iforce.praxis.results.controller;

import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.results.dto.RegisterResultDecisionRequest;
import br.com.iforce.praxis.results.dto.ResultDetailResponse;
import br.com.iforce.praxis.results.dto.ResultsPageResponse;
import br.com.iforce.praxis.results.service.ResultsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/results")
public class ResultsController {

    private final ResultsService resultsService;

    public ResultsController(ResultsService resultsService) {
        this.resultsService = resultsService;
    }

    @GetMapping
    public ResponseEntity<ResultsPageResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String simulationId,
            @RequestParam(required = false) AttemptStatus status,
            @RequestParam(required = false) String integrationProvider,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(resultsService.list(
                search,
                simulationId,
                status,
                integrationProvider,
                periodStart,
                periodEnd,
                page,
                size
        ));
    }

    @GetMapping("/{attemptId}")
    public ResponseEntity<ResultDetailResponse> get(@PathVariable String attemptId) {
        return ResponseEntity.ok(resultsService.get(attemptId));
    }

    @PostMapping("/{attemptId}/decision")
    public ResponseEntity<Void> registerDecision(
            @PathVariable String attemptId,
            @Valid @RequestBody RegisterResultDecisionRequest request
    ) {
        resultsService.registerDecision(attemptId, request);
        return ResponseEntity.noContent().build();
    }
}
