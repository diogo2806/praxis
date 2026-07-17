package br.com.iforce.praxis.journey.controller;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.candidate.controller.CandidateAttemptController;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.config.PraxisProperties;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptEntity;
import br.com.iforce.praxis.journey.persistence.entity.AssessmentJourneyAttemptStepEntity;
import br.com.iforce.praxis.journey.persistence.repository.AssessmentJourneyAttemptRepository;
import br.com.iforce.praxis.shared.security.EmpresaSecurity;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;

/**
 * Acrescenta o retorno para a Jornada de Avaliacao quando uma tentativa individual
 * vinculada a uma etapa e concluida. A URL inclui a etapa concluida para que o
 * frontend confirme o progresso e abra automaticamente a proxima avaliacao.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CandidateAttemptController.class)
public class CandidateJourneyRedirectAdvice implements ResponseBodyAdvice<Object> {

    private static final String ATTEMPT_PATH_MARKER = "/candidate/attempts/";

    private final JwtService jwtService;
    private final PraxisProperties praxisProperties;
    private final AssessmentJourneyAttemptRepository journeyAttemptRepository;

    public CandidateJourneyRedirectAdvice(
            JwtService jwtService,
            PraxisProperties praxisProperties,
            AssessmentJourneyAttemptRepository journeyAttemptRepository
    ) {
        this.jwtService = jwtService;
        this.praxisProperties = praxisProperties;
        this.journeyAttemptRepository = journeyAttemptRepository;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
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
            return enrich(participation, request);
        }
        if (body instanceof RegistrarRespostaResponse answer) {
            return enrich(answer, request);
        }
        return body;
    }

    private ParticipacaoResponse enrich(ParticipacaoResponse response, ServerHttpRequest request) {
        if (!response.finalizado() || hasText(response.redirectUrl())) {
            return response;
        }
        String redirectUrl = resolveJourneyRedirect(request);
        if (redirectUrl == null) {
            return response;
        }
        return new ParticipacaoResponse(
                response.participacaoId(),
                response.avaliacaoNome(),
                response.status(),
                response.finalizado(),
                redirectUrl,
                response.acaoSugeridaFrontend(),
                response.progresso(),
                response.etapaAtual(),
                response.verticalSaude()
        );
    }

    private RegistrarRespostaResponse enrich(RegistrarRespostaResponse response, ServerHttpRequest request) {
        if (!response.finalizado() || hasText(response.redirectUrl())) {
            return response;
        }
        String redirectUrl = resolveJourneyRedirect(request);
        if (redirectUrl == null) {
            return response;
        }
        return new RegistrarRespostaResponse(
                response.participacaoId(),
                response.status(),
                response.repetida(),
                response.finalizado(),
                redirectUrl,
                response.progresso(),
                response.etapaAtual()
        );
    }

    private String resolveJourneyRedirect(ServerHttpRequest request) {
        String token = extractAttemptToken(request.getURI().getPath());
        String attemptId = resolveAttemptId(token);
        if (attemptId == null) {
            return null;
        }

        String empresaId = EmpresaSecurity.requiredEmpresa();
        List<AssessmentJourneyAttemptEntity> journeys = journeyAttemptRepository
                .findDistinctByEmpresaIdAndStepsCandidateAttemptIdOrderByCreatedAtDesc(empresaId, attemptId);

        for (AssessmentJourneyAttemptEntity journey : journeys) {
            AssessmentJourneyAttemptStepEntity matchingStep = journey.getSteps().stream()
                    .filter(step -> attemptId.equals(step.getCandidateAttemptId()))
                    .findFirst()
                    .orElse(null);
            if (matchingStep != null && matchingStep.getId() != null) {
                return journeyPageBaseUrl()
                        + "/jornada/"
                        + journey.getId()
                        + "?completedStepId="
                        + matchingStep.getId();
            }
        }
        return null;
    }

    private String extractAttemptToken(String path) {
        int markerIndex = path.indexOf(ATTEMPT_PATH_MARKER);
        if (markerIndex < 0) {
            return null;
        }
        String remainder = path.substring(markerIndex + ATTEMPT_PATH_MARKER.length());
        int slashIndex = remainder.indexOf('/');
        return slashIndex < 0 ? remainder : remainder.substring(0, slashIndex);
    }

    private String resolveAttemptId(String token) {
        if (!hasText(token)) {
            return null;
        }
        try {
            return jwtService.parseCandidateAttemptToken(token).attemptId();
        } catch (RuntimeException exception) {
            return token.matches("att_[A-Za-z0-9]{16,64}") ? token : null;
        }
    }

    private String journeyPageBaseUrl() {
        return praxisProperties.candidatePageBaseUrl().replaceAll("/+$", "");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
