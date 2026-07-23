package br.com.iforce.praxis.results.controller;

import br.com.iforce.praxis.results.dto.ResultExecutiveReportResponse;
import br.com.iforce.praxis.results.dto.SaveInterviewGuideRequest;
import br.com.iforce.praxis.results.service.ResultExecutiveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API do relatório executivo e do roteiro de entrevista estruturada. */
@RestController
@RequestMapping("/api/v1/results")
public class ResultExecutiveController {

    private final ResultExecutiveService resultExecutiveService;

    public ResultExecutiveController(ResultExecutiveService resultExecutiveService) {
        this.resultExecutiveService = resultExecutiveService;
    }

    @GetMapping("/{attemptId}/executive-report")
    public ResponseEntity<ResultExecutiveReportResponse> get(@PathVariable String attemptId) {
        return ResponseEntity.ok(resultExecutiveService.get(attemptId));
    }

    @PostMapping("/{attemptId}/interview-guide")
    public ResponseEntity<Void> saveInterviewGuide(
            @PathVariable String attemptId,
            @Valid @RequestBody SaveInterviewGuideRequest request
    ) {
        resultExecutiveService.saveInterviewGuide(attemptId, request);
        return ResponseEntity.noContent().build();
    }
}
