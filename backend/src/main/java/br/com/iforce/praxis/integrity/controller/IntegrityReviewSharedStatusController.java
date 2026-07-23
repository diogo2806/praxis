package br.com.iforce.praxis.integrity.controller;

import br.com.iforce.praxis.integrity.dto.IntegrityReviewSharedStatusResponse;
import br.com.iforce.praxis.integrity.service.IntegrityReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/results")
@Tag(name = "Results", description = "Status neutro aprovado para o relatório empresarial.")
public class IntegrityReviewSharedStatusController {

    private final IntegrityReviewService integrityReviewService;

    public IntegrityReviewSharedStatusController(IntegrityReviewService integrityReviewService) {
        this.integrityReviewService = integrityReviewService;
    }

    @GetMapping("/{attemptId}/integrity-status")
    @Operation(
            summary = "Consulta status técnico compartilhado",
            description = "Retorna somente o parecer neutro e a data quando o revisor autorizou o compartilhamento."
    )
    public ResponseEntity<IntegrityReviewSharedStatusResponse> sharedStatus(@PathVariable String attemptId) {
        Optional<IntegrityReviewSharedStatusResponse> status = integrityReviewService.sharedStatus(attemptId);
        return status.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
