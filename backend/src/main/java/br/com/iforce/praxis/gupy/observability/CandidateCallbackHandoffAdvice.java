package br.com.iforce.praxis.gupy.observability;

import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.shared.security.EmpresaSecurity;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Registra no backend cada vez que uma conclusão é devolvida ao navegador com o callback.
 * A consulta da participação permanece idempotente e volta a oferecer a mesma URL, permitindo
 * recuperação ao reabrir o link quando a navegação anterior foi interrompida.
 */
@RestControllerAdvice
public class CandidateCallbackHandoffAdvice implements ResponseBodyAdvice<Object> {

    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;

    public CandidateCallbackHandoffAdvice(AuditEventService auditEventService, AuditMetadata auditMetadata) {
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> type = returnType.getParameterType();
        return ParticipacaoResponse.class.isAssignableFrom(type)
                || RegistrarRespostaResponse.class.isAssignableFrom(type);
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        if (body instanceof ParticipacaoResponse participation) {
            record(participation.participacaoId(), participation.finalizado(), participation.redirectUrl(), request);
        } else if (body instanceof RegistrarRespostaResponse answer) {
            record(answer.participacaoId(), answer.finalizado(), answer.redirectUrl(), request);
        }
        return body;
    }

    private void record(String attemptId, boolean completed, String redirectUrl, ServerHttpRequest request) {
        if (!completed || redirectUrl == null || redirectUrl.isBlank()) {
            return;
        }
        auditEventService.appendCandidateAttemptEvent(
                EmpresaSecurity.requiredEmpresa(),
                attemptId,
                AuditEventType.INTEGRATION_ACTIVITY_RECORDED,
                "Callback de conclusão disponibilizado ao navegador.",
                auditMetadata.of(
                        "handoff", "callback_presented",
                        "requestPath", request.getURI().getPath(),
                        "callbackHost", java.net.URI.create(redirectUrl).getHost()
                )
        );
    }
}
