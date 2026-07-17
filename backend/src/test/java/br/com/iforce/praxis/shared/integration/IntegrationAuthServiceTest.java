package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class IntegrationAuthServiceTest {

    private final IntegrationTokenRepository repository = mock(IntegrationTokenRepository.class);
    private final IntegrationAuthService service = new IntegrationAuthService(repository);

    @Test
    void rejectsMissingAuthorizationHeader() {
        assertUnauthorizedWithMandatoryMessage(null);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsAuthorizationHeaderWithoutBearerPrefix() {
        assertUnauthorizedWithMandatoryMessage("Basic abc");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsBlankBearerTokenBeforeHashing() {
        assertInvalidToken("Bearer   ");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsBearerTokenContainingWhitespaceBeforeHashing() {
        assertInvalidToken("Bearer abc def");
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsOversizedBearerTokenBeforeHashing() {
        assertInvalidToken("Bearer " + "a".repeat(513));
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsUnknownBearerToken() {
        String token = "integration-secret";
        when(repository.findFirstByProviderAndTokenHash("gupy", sha256(token)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateBearerToken("Bearer " + token, "gupy"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isEqualTo("Token Bearer inválido.");
                });
    }

    @Test
    void returnsEmpresaContextForValidToken() {
        String token = "integration-secret";
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("empresa-1");
        empresa.setCompanyId("company-99");

        IntegrationTokenEntity entity = new IntegrationTokenEntity();
        entity.setEmpresa(empresa);
        entity.setProvider("gupy");

        when(repository.findFirstByProviderAndTokenHash("gupy", sha256(token)))
                .thenReturn(Optional.of(entity));

        IntegrationEmpresaContext context = service.validateBearerToken("Bearer " + token, "gupy");

        assertThat(context.empresaId()).isEqualTo("empresa-1");
        assertThat(context.companyId()).isEqualTo("company-99");
        assertThat(context.provider()).isEqualTo("gupy");
        verify(repository).findFirstByProviderAndTokenHash("gupy", sha256(token));
    }

    @Test
    void hashesOnlyTokenValueAndKeepsProviderIsolation() {
        String token = "same-token";
        when(repository.findFirstByProviderAndTokenHash("recrutei", sha256(token)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateBearerToken("Bearer " + token, "recrutei"))
                .isInstanceOf(ResponseStatusException.class);

        verify(repository).findFirstByProviderAndTokenHash("recrutei", sha256(token));
    }

    private void assertUnauthorizedWithMandatoryMessage(String authorizationHeader) {
        assertThatThrownBy(() -> service.validateBearerToken(authorizationHeader, "gupy"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isEqualTo("Token Bearer obrigatório.");
                });
    }

    private void assertInvalidToken(String authorizationHeader) {
        assertThatThrownBy(() -> service.validateBearerToken(authorizationHeader, "gupy"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(exception.getReason()).isEqualTo("Token Bearer inválido.");
                });
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
