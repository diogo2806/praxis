package br.com.iforce.praxis.simulation.controller;

import br.com.iforce.praxis.simulation.dto.DecisionThresholdRequest;
import br.com.iforce.praxis.simulation.dto.DecisionThresholdResponse;
import br.com.iforce.praxis.simulation.dto.NormativeGroupRequest;
import br.com.iforce.praxis.simulation.dto.NormativeReferenceResponse;
import br.com.iforce.praxis.simulation.dto.TalentReferenceConfigurationResponse;
import br.com.iforce.praxis.simulation.service.TalentReferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/results/talent-references/simulations/{simulationId}/versions/{versionNumber}")
@Tag(name = "Talent Match - Referências", description = "Configuração segregada de perfil-alvo, grupo normativo e nota de corte.")
public class TalentReferenceController {

    private final TalentReferenceService talentReferenceService;

    public TalentReferenceController(TalentReferenceService talentReferenceService) {
        this.talentReferenceService = talentReferenceService;
    }

    @GetMapping
    @Operation(summary = "Consulta as referências atuais e históricas configuradas para a versão")
    public TalentReferenceConfigurationResponse getConfiguration(
            @PathVariable String simulationId,
            @PathVariable int versionNumber
    ) {
        return talentReferenceService.getConfiguration(simulationId, versionNumber);
    }

    @PostMapping("/normative-groups")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria e, quando elegível, ativa um grupo normativo da versão")
    public NormativeReferenceResponse configureNormativeGroup(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @Valid @RequestBody NormativeGroupRequest request
    ) {
        return talentReferenceService.configureNormativeGroup(simulationId, versionNumber, request);
    }

    @PostMapping("/decision-thresholds")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria e opcionalmente aprova uma nota de corte versionada")
    public DecisionThresholdResponse configureDecisionThreshold(
            @PathVariable String simulationId,
            @PathVariable int versionNumber,
            @Valid @RequestBody DecisionThresholdRequest request
    ) {
        return talentReferenceService.configureDecisionThreshold(simulationId, versionNumber, request);
    }
}
