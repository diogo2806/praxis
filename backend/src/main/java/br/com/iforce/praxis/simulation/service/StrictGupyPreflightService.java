package br.com.iforce.praxis.simulation.service;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.shared.integration.IntegrationTokenRepository;
import br.com.iforce.praxis.simulation.dto.GupyPreflightCheckResponse;
import br.com.iforce.praxis.simulation.dto.GupyPreflightResponse;
import br.com.iforce.praxis.simulation.model.GupyPreflightCheckCode;
import br.com.iforce.praxis.simulation.model.GupyPreflightCheckStatus;
import br.com.iforce.praxis.simulation.model.SimulationVersionStatus;
import br.com.iforce.praxis.simulation.persistence.entity.SimulationVersionEntity;
import br.com.iforce.praxis.simulation.persistence.repository.SimulationVersionRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Usa a mesma fonte de verdade da autenticação dos endpoints /test/** e só
 * aprova a ativação Gupy para versões efetivamente publicadas.
 */
@Primary
@Service
public class StrictGupyPreflightService extends GupyPreflightService {

    private static final String GUPY_PROVIDER = "gupy";

    private final CurrentEmpresaService currentEmpresaService;
    private final IntegrationTokenRepository integrationTokenRepository;

    public StrictGupyPreflightService(
            SimulationVersionRepository simulationVersionRepository,
            SimulationValidationService simulationValidationService,
            PraxisProperties praxisProperties,
            CurrentEmpresaService currentEmpresaService,
            EmpresaRepository empresaRepository,
            IntegrationTokenRepository integrationTokenRepository
    ) {
        super(
                simulationVersionRepository,
                simulationValidationService,
                praxisProperties,
                currentEmpresaService,
                empresaRepository
        );
        this.currentEmpresaService = currentEmpresaService;
        this.integrationTokenRepository = integrationTokenRepository;
    }

    @Override
    public GupyPreflightResponse evaluate(SimulationVersionEntity version) {
        GupyPreflightResponse base = super.evaluate(version);
        List<GupyPreflightCheckResponse> checks = new ArrayList<>();

        checks.add(publicationStatus(version));
        for (GupyPreflightCheckResponse check : base.checks()) {
            if (check.code() == GupyPreflightCheckCode.INTEGRATION_TOKEN) {
                checks.add(integrationToken());
            } else {
                checks.add(check);
            }
        }

        boolean ok = checks.stream()
                .noneMatch(check -> check.status() == GupyPreflightCheckStatus.BLOCKER);
        return new GupyPreflightResponse(
                base.simulationId(),
                base.versionNumber(),
                ok,
                List.copyOf(checks)
        );
    }

    private GupyPreflightCheckResponse publicationStatus(SimulationVersionEntity version) {
        if (version.getStatus() != SimulationVersionStatus.PUBLISHED) {
            return new GupyPreflightCheckResponse(
                    GupyPreflightCheckCode.PUBLICATION_STATUS,
                    GupyPreflightCheckStatus.BLOCKER,
                    "Publique a versão no Práxis antes de ativá-la no catálogo da Gupy."
            );
        }
        return new GupyPreflightCheckResponse(
                GupyPreflightCheckCode.PUBLICATION_STATUS,
                GupyPreflightCheckStatus.OK,
                "Versão publicada e disponível para ativação na Gupy."
        );
    }

    private GupyPreflightCheckResponse integrationToken() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        boolean configured = integrationTokenRepository
                .findFirstByEmpresaIdAndProvider(empresaId, GUPY_PROVIDER)
                .isPresent();
        if (!configured) {
            return new GupyPreflightCheckResponse(
                    GupyPreflightCheckCode.INTEGRATION_TOKEN,
                    GupyPreflightCheckStatus.BLOCKER,
                    "Gere e configure um token Gupy na Central de Integrações antes da ativação."
            );
        }
        return new GupyPreflightCheckResponse(
                GupyPreflightCheckCode.INTEGRATION_TOKEN,
                GupyPreflightCheckStatus.OK,
                "Token Gupy configurado na mesma fonte usada pela autenticação da integração."
        );
    }
}
