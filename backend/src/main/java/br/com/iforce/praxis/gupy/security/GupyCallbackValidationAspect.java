package br.com.iforce.praxis.gupy.security;

import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import br.com.iforce.praxis.shared.integration.IntegrationEmpresaContext;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/** Aplica a política específica da Gupy sem afetar provedores que reutilizam o serviço. */
@Aspect
@Component
public class GupyCallbackValidationAspect {

    private final GupyCallbackUrlPolicy callbackUrlPolicy;

    public GupyCallbackValidationAspect(GupyCallbackUrlPolicy callbackUrlPolicy) {
        this.callbackUrlPolicy = callbackUrlPolicy;
    }

    @Before(
            value = "execution(* br.com.iforce.praxis.gupy.service.CandidateAttemptService.createOrReuse(..))"
                    + " && args(request, empresaContext)",
            argNames = "request,empresaContext"
    )
    public void validate(CreateCandidateRequest request, IntegrationEmpresaContext empresaContext) {
        if (empresaContext != null && "gupy".equalsIgnoreCase(empresaContext.provider())) {
            callbackUrlPolicy.validate(request.callbackUrl());
        }
    }
}
