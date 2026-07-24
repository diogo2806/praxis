package br.com.iforce.praxis.enterpriseauth.filter;

import br.com.iforce.praxis.enterpriseauth.service.EnterpriseIdentityProviderService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class EnterprisePasswordLoginGuardFilter extends OncePerRequestFilter {

    private final EnterpriseIdentityProviderService identityProviderService;
    private final ObjectMapper objectMapper;

    public EnterprisePasswordLoginGuardFilter(
            EnterpriseIdentityProviderService identityProviderService,
            ObjectMapper objectMapper
    ) {
        this.identityProviderService = identityProviderService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !"/api/v1/auth/login".equals(resolvePath(request));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        CachedBodyRequest cachedRequest = new CachedBodyRequest(request);
        JsonNode body = objectMapper.readTree(cachedRequest.body());
        String email = body.path("email").asText("");
        if (!email.isBlank() && identityProviderService.isPasswordLoginBlocked(email)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), java.util.Map.of(
                    "code", "SSO_REQUIRED",
                    "message", "Este domínio exige acesso corporativo com SSO e MFA.",
                    "discoveryUrl", "/api/v1/enterprise-auth/discovery?email="
                            + java.net.URLEncoder.encode(email, java.nio.charset.StandardCharsets.UTF_8)
            ));
            return;
        }
        filterChain.doFilter(cachedRequest, response);
    }

    private String resolvePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        return contextPath == null || contextPath.isBlank()
                ? requestUri
                : requestUri.substring(contextPath.length());
    }

    private static final class CachedBodyRequest extends HttpServletRequestWrapper {

        private final byte[] body;

        private CachedBodyRequest(HttpServletRequest request) throws IOException {
            super(request);
            this.body = request.getInputStream().readAllBytes();
        }

        private byte[] body() {
            return body;
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream input = new ByteArrayInputStream(body);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return input.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                    // Leitura síncrona; não há callback assíncrono.
                }

                @Override
                public int read() {
                    return input.read();
                }
            };
        }
    }
}
