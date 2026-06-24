package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.simulation.dto.GupyPreflightCheckResponse;
import br.com.iforce.praxis.simulation.dto.GupyPreflightResponse;
import br.com.iforce.praxis.simulation.dto.SimulationValidationResponse;
import br.com.iforce.praxis.simulation.model.GupyPreflightCheckCode;
import br.com.iforce.praxis.simulation.model.GupyPreflightCheckStatus;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class GupyPreflightService {

    private final SimulationVersionRepository simulationVersionRepository;
    private final SimulationValidationService simulationValidationService;
    private final PraxisProperties praxisProperties;
    private final CurrentTenantService currentTenantService;
    private final TenantRepository tenantRepository;

    public GupyPreflightService(
            SimulationVersionRepository simulationVersionRepository,
            SimulationValidationService simulationValidationService,
            PraxisProperties praxisProperties,
            CurrentTenantService currentTenantService,
            TenantRepository tenantRepository
    ) {
        this.simulationVersionRepository = simulationVersionRepository;
        this.simulationValidationService = simulationValidationService;
        this.praxisProperties = praxisProperties;
        this.currentTenantService = currentTenantService;
        this.tenantRepository = tenantRepository;
    }

    @Transactional(readOnly = true)
    public GupyPreflightResponse getPreflight(String simulationId, int versionNumber) {
        SimulationVersionEntity simulationVersionEntity = findVersion(simulationId, versionNumber);
        if (simulationVersionEntity.getStatus() != SimulationVersionStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Somente versões publicadas podem passar no preflight Gupy.");
        }

        return evaluate(simulationVersionEntity);
    }

    private SimulationVersionEntity findVersion(String simulationId, int versionNumber) {
        String tenantId = currentTenantService.requiredTenantId();
        return simulationVersionRepository
                .findBySimulationTenantIdAndSimulationIdAndVersionNumber(tenantId, simulationId, versionNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Versão de simulação não encontrada."));
    }

    public GupyPreflightResponse evaluate(SimulationVersionEntity simulationVersionEntity) {
        List<GupyPreflightCheckResponse> checks = new ArrayList<>();
        checks.add(validatePublicBaseUrl());
        checks.add(validateIntegrationToken());
        checks.add(validateSimulation(simulationVersionEntity));

        boolean ok = checks.stream()
                .noneMatch(check -> GupyPreflightCheckStatus.BLOCKER.equals(check.status()));

        return new GupyPreflightResponse(
                simulationVersionEntity.getSimulation().getId(),
                simulationVersionEntity.getVersionNumber(),
                ok,
                checks
        );
    }

    private GupyPreflightCheckResponse validatePublicBaseUrl() {
        String publicBaseUrl = praxisProperties.publicBaseUrl();
        if (isBlank(publicBaseUrl)) {
            return blocker(GupyPreflightCheckCode.PUBLIC_BASE_URL, "URL pública do backend não configurada.");
        }

        try {
            URI uri = URI.create(publicBaseUrl);
            boolean hasHttpScheme = "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
            if (!hasHttpScheme || isBlank(uri.getHost())) {
                return blocker(GupyPreflightCheckCode.PUBLIC_BASE_URL, "URL pública deve usar http ou https e conter host.");
            }
        } catch (IllegalArgumentException exception) {
            return blocker(GupyPreflightCheckCode.PUBLIC_BASE_URL, "URL pública inválida.");
        }

        return ok(GupyPreflightCheckCode.PUBLIC_BASE_URL, "URL pública configurada.");
    }

    private GupyPreflightCheckResponse validateIntegrationToken() {
        String tenantId = currentTenantService.requiredTenantId();
        return tenantRepository.findById(tenantId)
                .filter(tenant -> !isBlank(tenant.getIntegrationTokenHash()))
                .map(tenant -> ok(GupyPreflightCheckCode.INTEGRATION_TOKEN, "Token de integração configurado no tenant."))
                .orElseGet(() -> warning(
                        GupyPreflightCheckCode.INTEGRATION_TOKEN,
                        "Token de integração Gupy não configurado no tenant. A simulação será publicada, mas o envio de resultados para a Gupy ficará indisponível até a integração ser configurada."
                ));
    }

    private GupyPreflightCheckResponse validateSimulation(SimulationVersionEntity simulationVersionEntity) {
        SimulationValidationResponse validationResponse = simulationValidationService.validate(simulationVersionEntity);
        if (!validationResponse.publishable()) {
            return blocker(GupyPreflightCheckCode.SIMULATION_VALIDATION, "Versão possui blockers no validador.");
        }

        if (!validationResponse.issues().isEmpty()) {
            return new GupyPreflightCheckResponse(
                    GupyPreflightCheckCode.SIMULATION_VALIDATION,
                    GupyPreflightCheckStatus.WARNING,
                    "Versão publicável com avisos do validador."
            );
        }

        return ok(GupyPreflightCheckCode.SIMULATION_VALIDATION, "Versão publicável sem avisos.");
    }

    private GupyPreflightCheckResponse ok(GupyPreflightCheckCode code, String message) {
        return new GupyPreflightCheckResponse(code, GupyPreflightCheckStatus.OK, message);
    }

    private GupyPreflightCheckResponse warning(GupyPreflightCheckCode code, String message) {
        return new GupyPreflightCheckResponse(code, GupyPreflightCheckStatus.WARNING, message);
    }

    private GupyPreflightCheckResponse blocker(GupyPreflightCheckCode code, String message) {
        return new GupyPreflightCheckResponse(code, GupyPreflightCheckStatus.BLOCKER, message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
