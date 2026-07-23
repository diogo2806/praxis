package br.com.iforce.praxis.featureflag.controller;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.EvaluationRequest;
import br.com.iforce.praxis.featureflag.dto.FeatureFlagContracts.FrontendFlagsResponse;
import br.com.iforce.praxis.featureflag.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/feature-flags")
@Tag(name = "Feature flags", description = "Flags frontend avaliadas sem exposição das regras internas.")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final EmpresaRepository empresaRepository;

    public FeatureFlagController(
            FeatureFlagService featureFlagService,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            EmpresaRepository empresaRepository
    ) {
        this.featureFlagService = featureFlagService;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.empresaRepository = empresaRepository;
    }

    @GetMapping
    @Operation(summary = "Retorna somente as flags liberadas ao frontend")
    public ResponseEntity<FrontendFlagsResponse> frontendFlags(Authentication authentication) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String userId = currentUserService.requiredUserId();
        String plan = empresaRepository.findById(empresaId)
                .map(EmpresaEntity::getCommercialPlanType)
                .map(Enum::name)
                .orElse(null);
        Set<String> roles = authentication == null
                ? Set.of()
                : authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                        .collect(Collectors.toUnmodifiableSet());
        EvaluationRequest context = new EvaluationRequest(
                empresaId,
                plan,
                userId,
                roles,
                null,
                userId
        );
        return ResponseEntity.ok(featureFlagService.frontendFlags(context));
    }
}
