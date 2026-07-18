package br.com.iforce.praxis.gupy.controller;

import br.com.iforce.praxis.gupy.dto.GupyHomologationResponse;
import br.com.iforce.praxis.gupy.service.GupyHomologationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoint administrativo com a prontidão e as evidências da homologação Gupy. */
@RestController
@RequestMapping("/api/v1/integrations/gupy/homologation")
@Tag(name = "Gupy Homologation", description = "Prontidão técnica e evidências da integração Gupy.")
public class GupyHomologationController {

    private final GupyHomologationService gupyHomologationService;

    public GupyHomologationController(GupyHomologationService gupyHomologationService) {
        this.gupyHomologationService = gupyHomologationService;
    }

    @GetMapping
    @Operation(
            summary = "Consulta prontidão da homologação Gupy",
            description = "Separa bloqueios internos, evidências automáticas e etapas que dependem de uma vaga real na Gupy."
    )
    public ResponseEntity<GupyHomologationResponse> getStatus() {
        return ResponseEntity.ok(gupyHomologationService.getStatus());
    }
}
