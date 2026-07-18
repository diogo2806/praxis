package br.com.iforce.praxis.partner.service;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.gupy.dto.GupyTestResponse;
import br.com.iforce.praxis.gupy.dto.TestItemsResponse;
import br.com.iforce.praxis.gupy.service.GupyTestCatalogMapper;
import br.com.iforce.praxis.recrutei.dto.RecruteiTestListResponse;
import br.com.iforce.praxis.recrutei.dto.RecruteiTestResponse;
import br.com.iforce.praxis.shared.integration.IntegrationAuthService;
import br.com.iforce.praxis.shared.integration.IntegrationEmpresaContext;
import br.com.iforce.praxis.shared.integration.IntegrationManagementService;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

@Aspect
@Component
public class PartnerIntegrationCatalogAspect {

    private static final String GUPY_PROVIDER = "gupy";
    private static final String RECRUTEI_PROVIDER = "recrutei";

    private final IntegrationAuthService integrationAuthService;
    private final PartnerIntegrationCatalogService partnerCatalogService;
    private final GupyTestCatalogMapper gupyTestCatalogMapper;
    private final IntegrationManagementService integrationManagementService;

    public PartnerIntegrationCatalogAspect(
            IntegrationAuthService integrationAuthService,
            PartnerIntegrationCatalogService partnerCatalogService,
            GupyTestCatalogMapper gupyTestCatalogMapper,
            IntegrationManagementService integrationManagementService
    ) {
        this.integrationAuthService = integrationAuthService;
        this.partnerCatalogService = partnerCatalogService;
        this.gupyTestCatalogMapper = gupyTestCatalogMapper;
        this.integrationManagementService = integrationManagementService;
    }

    @Around("execution(* br.com.iforce.praxis.gupy.controller.GupyIntegrationController.listPublishedTests(..)) "
            + "&& args(authorization,searchString,offset,limit)")
    public Object listGupyCatalog(
            ProceedingJoinPoint joinPoint,
            String authorization,
            String searchString,
            int offset,
            int limit
    ) throws Throwable {
        IntegrationEmpresaContext context = integrationAuthService.validateBearerToken(
                authorization,
                GUPY_PROVIDER
        );
        if (context.partnerClientId() == null) {
            return joinPoint.proceed();
        }

        int normalizedOffset = Math.max(offset, 0);
        int normalizedLimit = Math.min(Math.max(limit, 0), 400);
        List<GupyTestResponse> tests = normalizedLimit == 0
                ? List.of()
                : partnerCatalogService.findPublished(
                        context.empresaId(),
                        context.partnerClientId(),
                        searchString,
                        normalizedOffset,
                        normalizedLimit
                ).stream().map(gupyTestCatalogMapper::toResponse).toList();
        int total = partnerCatalogService.countPublished(
                context.empresaId(),
                context.partnerClientId(),
                searchString
        );
        integrationManagementService.recordActivity(
                context.empresaId(),
                IntegrationProvider.GUPY,
                "GET /test [partner-client=" + context.partnerClientId() + "]"
        );
        return ResponseEntity.ok(new TestItemsResponse(normalizedLimit, normalizedOffset, total, tests));
    }

    @Around("execution(* br.com.iforce.praxis.recrutei.controller.RecruteiIntegrationController.listPublishedTests(..)) "
            + "&& args(authorization,search,offset,limit)")
    public Object listRecruteiCatalog(
            ProceedingJoinPoint joinPoint,
            String authorization,
            String search,
            int offset,
            int limit
    ) throws Throwable {
        IntegrationEmpresaContext context = integrationAuthService.validateBearerToken(
                authorization,
                RECRUTEI_PROVIDER
        );
        if (context.partnerClientId() == null) {
            return joinPoint.proceed();
        }

        int normalizedOffset = Math.max(offset, 0);
        int normalizedLimit = Math.min(Math.max(limit, 1), 400);
        List<RecruteiTestResponse> tests = partnerCatalogService.findPublished(
                context.empresaId(),
                context.partnerClientId(),
                search,
                normalizedOffset,
                normalizedLimit
        ).stream().map(simulation -> new RecruteiTestResponse(
                simulation.id(),
                simulation.name(),
                "Situational Judgment",
                simulation.description(),
                "advanced"
        )).toList();
        int total = partnerCatalogService.countPublished(
                context.empresaId(),
                context.partnerClientId(),
                search
        );
        integrationManagementService.recordActivity(
                context.empresaId(),
                IntegrationProvider.RECRUTEI,
                "GET /recrutei/test [partner-client=" + context.partnerClientId() + "]"
        );
        return ResponseEntity.ok(new RecruteiTestListResponse(
                normalizedLimit,
                normalizedOffset,
                total,
                tests
        ));
    }

    @Before("execution(* br.com.iforce.praxis.gupy.service.CandidateAttemptService.createOrReuse(..)) "
            + "&& args(request,context)")
    public void assertCandidateTestAccess(
            CreateCandidateRequest request,
            IntegrationEmpresaContext context
    ) {
        if (context.partnerClientId() == null) {
            return;
        }
        partnerCatalogService.assertAccess(
                context.empresaId(),
                context.partnerClientId(),
                request.testId()
        );
    }
}
