package br.com.iforce.praxis.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void deveIgnorarCabecalhoForjadoEmAcessoDireto() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.7");

        assertThat(resolver.resolve(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void deveUsarEnderecoJaNormalizadoPeloContainer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("198.51.100.7");
        request.addHeader("X-Forwarded-For", "198.51.100.7, 10.0.0.4");

        assertThat(resolver.resolve(request)).isEqualTo("198.51.100.7");
    }

    @Test
    void deveUsarIdentidadeComumQuandoEnderecoForInvalido() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(" ");

        assertThat(resolver.resolve(request)).isEqualTo("unknown");
    }

    @Test
    void deveLimitarComprimentoDoEndereco() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("1".repeat(65));

        assertThat(resolver.resolve(request)).isEqualTo("unknown");
    }
}
