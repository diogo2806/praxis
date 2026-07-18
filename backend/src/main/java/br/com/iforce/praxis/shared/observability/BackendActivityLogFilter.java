package br.com.iforce.praxis.shared.observability;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService.AuthenticatedEmpresa;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Gera um registro operacional para cada requisição HTTP concluída pelo backend.
 *
 * <p>O log usa o padrão de rota resolvido pelo Spring em vez da URI concreta. Isso
 * evita expor tokens, identificadores de candidatos e demais valores presentes em
 * parâmetros de caminho. Corpo, query string, cookies e cabeçalhos de autenticação
 * nunca são registrados.</p>
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 10)
public class BackendActivityLogFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-Id";
    static final String REQUEST_ID_MDC_KEY = "requestId";
    private static final String ACTIVITY_LOG_PREFIX = "atividade_http";
    private static final String ANONYMOUS_USER = "anonymousUser";
    private static final int MAX_LOG_VALUE_LENGTH = 160;
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = resolveRequestId(request);
        String previousRequestId = MDC.get(REQUEST_ID_MDC_KEY);
        long startedAtNanos = System.nanoTime();
        Exception failure = null;

        MDC.put(REQUEST_ID_MDC_KEY, requestId);
        response.setHeader(REQUEST_ID_HEADER, requestId);

        try {
            filterChain.doFilter(request, response);
        } catch (IOException | ServletException | RuntimeException exception) {
            failure = exception;
            throw exception;
        } finally {
            logCompletedRequest(request, response, requestId, startedAtNanos, failure);
            restoreMdc(previousRequestId);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String requestUri = request.getRequestURI();
        return requestUri.equals("/actuator/health")
                || requestUri.equals("/actuator/info")
                || requestUri.startsWith("/v3/api-docs")
                || requestUri.startsWith("/swagger-ui")
                || requestUri.equals("/docs")
                || requestUri.startsWith("/docs/");
    }

    private void logCompletedRequest(
            HttpServletRequest request,
            HttpServletResponse response,
            String requestId,
            long startedAtNanos,
            Exception failure
    ) {
        long durationMilliseconds = Math.max(
                0L,
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAtNanos)
        );
        int status = response.getStatus();
        String method = safeLogValue(request.getMethod());
        String route = resolveRoute(request);
        String actor = resolveActor();
        String empresaId = resolveEmpresaId();
        String remoteAddress = safeLogValue(request.getRemoteAddr());
        String exceptionName = failure == null ? "-" : failure.getClass().getSimpleName();

        if (failure != null || status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            log.error(
                    "{} method={} route={} status={} durationMs={} requestId={} actor={} empresa={} remoteIp={} exception={}",
                    ACTIVITY_LOG_PREFIX,
                    method,
                    route,
                    status,
                    durationMilliseconds,
                    requestId,
                    actor,
                    empresaId,
                    remoteAddress,
                    exceptionName,
                    failure
            );
            return;
        }

        if (status >= HttpServletResponse.SC_BAD_REQUEST) {
            log.warn(
                    "{} method={} route={} status={} durationMs={} requestId={} actor={} empresa={} remoteIp={}",
                    ACTIVITY_LOG_PREFIX,
                    method,
                    route,
                    status,
                    durationMilliseconds,
                    requestId,
                    actor,
                    empresaId,
                    remoteAddress
            );
            return;
        }

        log.info(
                "{} method={} route={} status={} durationMs={} requestId={} actor={} empresa={} remoteIp={}",
                ACTIVITY_LOG_PREFIX,
                method,
                route,
                status,
                durationMilliseconds,
                requestId,
                actor,
                empresaId,
                remoteAddress
        );
    }

    private String resolveRoute(HttpServletRequest request) {
        Object routePattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (routePattern instanceof String pattern && !pattern.isBlank()) {
            return safeLogValue(pattern);
        }
        return safeLogValue(request.getRequestURI());
    }

    private String resolveActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || ANONYMOUS_USER.equals(authentication.getPrincipal())) {
            return "anonymous";
        }
        return safeLogValue(authentication.getName());
    }

    private String resolveEmpresaId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object details = authentication == null ? null : authentication.getDetails();
        if (details instanceof AuthenticatedEmpresa authenticatedEmpresa) {
            return safeLogValue(authenticatedEmpresa.empresaId());
        }
        if (details instanceof String empresaId) {
            return safeLogValue(empresaId);
        }
        return "-";
    }

    private String resolveRequestId(HttpServletRequest request) {
        String providedRequestId = request.getHeader(REQUEST_ID_HEADER);
        if (providedRequestId != null && VALID_REQUEST_ID.matcher(providedRequestId).matches()) {
            return providedRequestId;
        }
        return UUID.randomUUID().toString();
    }

    private String safeLogValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String sanitized = value.replace('\r', '_').replace('\n', '_');
        return sanitized.length() <= MAX_LOG_VALUE_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_LOG_VALUE_LENGTH);
    }

    private void restoreMdc(String previousRequestId) {
        if (previousRequestId == null) {
            MDC.remove(REQUEST_ID_MDC_KEY);
            return;
        }
        MDC.put(REQUEST_ID_MDC_KEY, previousRequestId);
    }
}
