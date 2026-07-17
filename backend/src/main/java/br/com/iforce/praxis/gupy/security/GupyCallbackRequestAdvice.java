package br.com.iforce.praxis.gupy.security;

import br.com.iforce.praxis.gupy.delivery.service.GupyOutboundUrlValidator;
import br.com.iforce.praxis.gupy.dto.CreateCandidateRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Type;

/** Valida as URLs externas assim que o contrato público da Gupy é desserializado. */
@RestControllerAdvice
public class GupyCallbackRequestAdvice extends RequestBodyAdviceAdapter {

    private final GupyCallbackUrlPolicy callbackUrlPolicy;
    private final GupyOutboundUrlValidator outboundUrlValidator;

    public GupyCallbackRequestAdvice(
            GupyCallbackUrlPolicy callbackUrlPolicy,
            GupyOutboundUrlValidator outboundUrlValidator
    ) {
        this.callbackUrlPolicy = callbackUrlPolicy;
        this.outboundUrlValidator = outboundUrlValidator;
    }

    @Override
    public boolean supports(
            MethodParameter methodParameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return targetType == CreateCandidateRequest.class;
    }

    @Override
    public Object afterBodyRead(
            Object body,
            HttpInputMessage inputMessage,
            MethodParameter parameter,
            Type targetType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        if (body instanceof CreateCandidateRequest request && request.contractCompanyId() != null) {
            callbackUrlPolicy.validate(request.callbackUrl());
            validateResultWebhookUrl(request);
        }
        return body;
    }

    private void validateResultWebhookUrl(CreateCandidateRequest request) {
        if (request.resultWebhookUrl() == null) {
            return;
        }
        try {
            outboundUrlValidator.validateForPersistence(request.resultWebhookUrl());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
