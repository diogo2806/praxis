package br.com.iforce.praxis.shared.observability;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService.AuthenticatedEmpresa;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class BackendActivityLogFilterTest {

    private final BackendActivityLogFilter backendActivityLogFilter = new BackendActivityLogFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logsCompletedActivityUsingRoutePatternWithoutSensitiveValues(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "POST",
                "/api/v1/simulations/sim_secreto/versions/1/publish"
        );
        request.setQueryString("token=segredo-na-query");
        request.addHeader("Authorization", "Bearer segredo-do-token");
        request.addHeader(BackendActivityLogFilter.REQUEST_ID_HEADER, "req-123");
        request.setRemoteAddr("10.0.0.15");
        MockHttpServletResponse response = new MockHttpServletResponse();

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "usuario@empresa.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPRESA"))
        );
        authentication.setDetails(new AuthenticatedEmpresa("empresa-1"));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        FilterChain filterChain = (servletRequest, servletResponse) -> {
            servletRequest.setAttribute(
                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                    "/api/v1/simulations/{simulationId}/versions/{versionNumber}/publish"
            );
            ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_NO_CONTENT);
        };

        backendActivityLogFilter.doFilter(request, response, filterChain);

        assertThat(response.getHeader(BackendActivityLogFilter.REQUEST_ID_HEADER)).isEqualTo("req-123");
        assertThat(output)
                .contains("atividade_http")
                .contains("method=POST")
                .contains("route=/api/v1/simulations/{simulationId}/versions/{versionNumber}/publish")
                .contains("status=204")
                .contains("requestId=req-123")
                .contains("actor=usuario@empresa.com")
                .contains("empresa=empresa-1")
                .contains("remoteIp=10.0.0.15")
                .doesNotContain("sim_secreto")
                .doesNotContain("segredo-na-query")
                .doesNotContain("segredo-do-token");
    }

    @Test
    void ignoresHealthChecks(CapturedOutput output) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (servletRequest, servletResponse) ->
                ((HttpServletResponse) servletResponse).setStatus(HttpServletResponse.SC_OK);

        backendActivityLogFilter.doFilter(request, response, filterChain);

        assertThat(output).doesNotContain("atividade_http");
    }
}
