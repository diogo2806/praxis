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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Usa a mesma fonte de verdade da autenticação dos endpoints /test/**.
 *
 * <p>A publicação de uma avaliação e a ativação no catálogo da Gupy são etapas
 * distintas. {@link #evaluate(SimulationVersionEntity)} é usada internamente
 * durante a publicação e, por isso, trata a ausência de token como aviso. Já
 * {@link #getPreflight(String, int)} representa a ativação Gupy: exige versão
 * publicada e token real configurado.</p>
 */
@Primary
@Service
public class StrictGupyPreflightService extends GupyPreflightService {

    private static final String GUPY_PROVIDER = "gupy";

    private final SimulationVersionRepository simulationVersionRepository;
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
        this.simulationVersionRepository = simulationVersionRepository;
        this.currentEmpresaService = currentEmpresaService;
        this.integrationTokenRepository = integrationTokenRepository;
    }

    /**
     * Checagem usada pela publicação. Mantém a validação estrutural e consulta
     * o repositório real de tokens, mas não obriga a empresa a usar Gupy para
     * publicar uma avaliação destinada a link direto ou outro ATS.
     */
    @Override
    public GupyPreflightResponse evaluate(SimulationVersionEntity version) {
        GupyPreflightResponse base = super.evaluate(version);
        return response(base, replaceTokenCheck(base.checks(), false));
    }

    /**
     * Checagem explícita de ativação Gupy. Aqui a versão precisa estar
     * publicada e o token é obrigatório.
     */
    @Override
    @Transactional(readOnly = true)
    public GupyPreflightResponse getPreflight(String simulationId, int versionNumber) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        SimulationVersionEntity version = simulationVersionRepository
                .findBySimulationEmpresaIdAndSimulationIdAndVersionNumber(
                        empresaId,
                        simulationId,
                        versionNumber
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Não encontramos esta versão do teste."
                ));
        if (version.getStatus() == SimulationVersionStatus.ARCHIVED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Versões arquivadas não podem passar no preflight Gupy."
            );
        }

        GupyPreflightResponse base = evaluate(version);
        List<GupyPreflightCheckResponse> checks = new ArrayList<>();
        checks.add(publicationStatus(version));
        checks.addAll(replaceTokenCheck(base.checks(), true));
        return response(base, checks);
    }

    private List<GupyPreflightCheckResponse> replaceTokenCheck(
            List<GupyPreflightCheckResponse> source,
            boolean required
    ) {
        List<GupyPreflightCheckResponse> checks = new ArrayList<>();
        for (GupyPreflightCheckResponse check : source) {
            if (check.code() == GupyPreflightCheckCode.INTEGRATION_TOKEN) {
                checks.add(integrationToken(required));
            } else {
                checks.add(check);
            }
        }
        return checks;
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

    private GupyPreflightResponse response(
            GupyPreflightResponse base,
            List<GupyPreflightCheckResponse> checks
    ) {
        boolean ok = checks.stream()
                .noneMatch(check -> check.status() == GupyPreflightCheckStatus.BLOCKER);
        return new GupyPreflightResponse(
                base.simulationId(),
                base.versionNumber(),
                ok,
                List.copyOf(checks)
        );
    }

    private GupyPreflightCheckResponse integrationToken(boolean required) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        boolean configured = integrationTokenRepository
                .findFirstByEmpresaIdAndProvider(empresaId, GUPY_PROVIDER)
                .isPresent();
        if (!configured) {
            return new GupyPreflightCheckResponse(
                    GupyPreflightCheckCode.INTEGRATION_TOKEN,
                    required ? GupyPreflightCheckStatus.BLOCKER : GupyPreflightCheckStatus.WARNING,
                    required
                            ? "Gere e configure um token Gupy na Central de Integrações antes da ativação."
                            : "Token Gupy ainda não configurado. A avaliação pode ser publicada, mas não ativada na Gupy."
            );
        }
        return new GupyPreflightCheckResponse(
                GupyPreflightCheckCode.INTEGRATION_TOKEN,
                GupyPreflightCheckStatus.OK,
                "Token Gupy configurado na mesma fonte usada pela autenticação da integração."
        );
    }
}
