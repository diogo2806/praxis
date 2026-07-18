package br.com.iforce.praxis.auth.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class PartnerSpecialistAuthorizationFilterTest {

    private final PartnerSpecialistAuthorizationFilter filter = new PartnerSpecialistAuthorizationFilter();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void permiteCriarEReditarAvaliacao() throws Exception {
        authenticateSpecialist();
        MockHttpServletRequest request = request("POST", "/api/v1/simulations/drafts");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void bloqueiaPublicacaoPeloEspecialista() throws Exception {
        authenticateSpecialist();
        MockHttpServletRequest request = request(
                "POST",
                "/api/v1/simulations/teste-1/versions/1/publish"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Especialistas podem criar e revisar avaliações");
        verifyNoInteractions(chain);
    }

    @Test
    void bloqueiaClientesIntegracoesResultadosECobranca() throws Exception {
        authenticateSpecialist();
        for (String path : List.of(
                "/api/v1/partners/clients",
                "/api/v1/integrations",
                "/api/v1/results",
                "/api/v1/billing"
        )) {
            MockHttpServletRequest request = request("GET", path);
            MockHttpServletResponse response = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);

            filter.doFilter(request, response, chain);

            assertThat(response.getStatus()).isEqualTo(403);
            verifyNoInteractions(chain);
        }
    }

    private void authenticateSpecialist() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "specialist-1",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_PARTNER_SPECIALIST"))
                )
        );
    }

    private MockHttpServletRequest request(String method, String path) {
        return new MockHttpServletRequest(method, path);
    }
}
