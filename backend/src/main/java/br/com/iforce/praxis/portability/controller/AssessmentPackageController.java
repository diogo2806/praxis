package br.com.iforce.praxis.portability.controller;

import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.ImportPackageRequest;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.ImportPackageResponse;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageEnvelope;
import br.com.iforce.praxis.portability.dto.AssessmentPackageDtos.PackageValidationResponse;
import br.com.iforce.praxis.portability.service.AssessmentPackageService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Assessment Portability", description = "Exportação e importação segura de avaliações versionadas.")
public class AssessmentPackageController {

    private final AssessmentPackageService assessmentPackageService;

    public AssessmentPackageController(AssessmentPackageService assessmentPackageService) {
        this.assessmentPackageService = assessmentPackageService;
    }

    @GetMapping(value = "/simulations/{simulationId}/versions/{versionNumber}/package", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Exporta uma avaliação como pacote portátil versionado")
    public ResponseEntity<PackageEnvelope> exportPackage(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        PackageEnvelope envelope = assessmentPackageService.exportPackage(simulationId, versionNumber);
        String filename = "praxis-" + simulationId + "-v" + versionNumber + ".json";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(envelope);
    }

    @PostMapping("/simulation-packages/validate")
    @Operation(summary = "Valida um pacote sem persistir dados")
    public ResponseEntity<PackageValidationResponse> validatePackage(
            @Valid @RequestBody PackageEnvelope envelope
    ) {
        return ResponseEntity.ok(assessmentPackageService.validate(envelope));
    }

    @PostMapping("/simulation-packages/import")
    @Operation(summary = "Importa um pacote validado como nova avaliação em rascunho")
    public ResponseEntity<ImportPackageResponse> importPackage(
            @Valid @RequestBody ImportPackageRequest request
    ) {
        return ResponseEntity.status(201).body(assessmentPackageService.importPackage(request));
    }
}
