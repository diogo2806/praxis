package br.com.iforce.praxis.integrity.controller;

import br.com.iforce.praxis.integrity.dto.IntegrityReviewDecisionRequest;
import br.com.iforce.praxis.integrity.dto.IntegrityReviewDetailResponse;
import br.com.iforce.praxis.integrity.dto.IntegrityReviewPageResponse;
import br.com.iforce.praxis.integrity.service.IntegrityReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrity-reviews")
@Tag(name = "Integrity Reviews", description = "Fila restrita de revisão humana das evidências técnicas.")
public class IntegrityReviewController {

    private final IntegrityReviewService integrityReviewService;

    public IntegrityReviewController(IntegrityReviewService integrityReviewService) {
        this.integrityReviewService = integrityReviewService;
    }

    @GetMapping
    @Operation(
            summary = "Lista revisões técnicas",
            description = "Retorna alertas determinísticos e neutros, sem alterar pontuação ou decisão de contratação."
    )
    public ResponseEntity<IntegrityReviewPageResponse> search(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return ResponseEntity.ok(integrityReviewService.search(page, size));
    }

    @GetMapping("/{attemptId}")
    @Operation(
            summary = "Abre evidências técnicas",
            description = "Registra o acesso e retorna sessões, linha do tempo, alertas explicáveis e trilha de alterações."
    )
    public ResponseEntity<IntegrityReviewDetailResponse> detail(@PathVariable String attemptId) {
        return ResponseEntity.ok(integrityReviewService.detail(attemptId));
    }

    @PostMapping("/{attemptId}/decision")
    @Operation(
            summary = "Registra parecer humano",
            description = "Exige justificativa e aceita somente pareceres neutros definidos pelo domínio."
    )
    public ResponseEntity<IntegrityReviewDetailResponse> decide(
            @PathVariable String attemptId,
            @Valid @RequestBody IntegrityReviewDecisionRequest request
    ) {
        return ResponseEntity.ok(integrityReviewService.decide(attemptId, request));
    }
}
