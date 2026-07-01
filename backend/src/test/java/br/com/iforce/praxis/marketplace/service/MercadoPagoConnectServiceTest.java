package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.billing.service.MercadoPagoClient;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoConnectServiceTest {

    private static final String STATE_SECRET = "12345678901234567890123456789012";

    @Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private MarketplaceProfessionalRepository professionalRepository;
    @Mock private MarketplaceTokenCryptoService tokenCryptoService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MercadoPagoConnectService service;

    @BeforeEach
    void setUp() {
        service = new MercadoPagoConnectService(
                mercadoPagoClient,
                professionalRepository,
                tokenCryptoService,
                STATE_SECRET
        );
    }

    @Test
    void handleCallbackAcceptsValidState() throws Exception {
        MarketplaceProfessionalEntity professional = professional(20L);
        when(professionalRepository.findById(20L)).thenReturn(Optional.of(professional));
        when(mercadoPagoClient.exchangeAuthorizationCode("auth-code"))
                .thenReturn(objectMapper.readTree("{\"access_token\":\"mp-token\",\"user_id\":\"seller-123\"}"));
        when(tokenCryptoService.encrypt("mp-token")).thenReturn("encrypted-token");

        service.handleCallback(validStateFor(20L, "nonce-valido"), "auth-code");

        assertThat(professional.getMpSellerId()).isEqualTo("seller-123");
        assertThat(professional.getMpAccessToken()).isEqualTo("encrypted-token");
        verify(mercadoPagoClient).exchangeAuthorizationCode("auth-code");
        verify(tokenCryptoService).encrypt("mp-token");
    }

    @Test
    void handleCallbackRejectsStateWithAlteredSignatureCharacter() throws Exception {
        String validState = validStateFor(20L, "nonce-valido");
        String tamperedState = validState.substring(0, validState.length() - 1)
                + (validState.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> service.handleCallback(tamperedState, "auth-code"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);

        verify(professionalRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        verify(mercadoPagoClient, never()).exchangeAuthorizationCode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void handleCallbackRejectsMalformedState() {
        assertThatThrownBy(() -> service.handleCallback("20:sem-assinatura", "auth-code"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.BAD_REQUEST);

        verify(professionalRepository, never()).findById(org.mockito.ArgumentMatchers.anyLong());
        verify(mercadoPagoClient, never()).exchangeAuthorizationCode(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void handleCallbackReturnsNotFoundWhenProfessionalDoesNotExistAndStateIsValid() {
        when(professionalRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.handleCallback(validStateFor(999L, "nonce-valido"), "auth-code"))
                .isInstanceOf(ResponseStatusException.class)
                .hasFieldOrPropertyWithValue("statusCode", HttpStatus.NOT_FOUND);

        verify(mercadoPagoClient, never()).exchangeAuthorizationCode(org.mockito.ArgumentMatchers.anyString());
    }

    private static MarketplaceProfessionalEntity professional(Long id) {
        MarketplaceProfessionalEntity professional = new MarketplaceProfessionalEntity();
        professional.setId(id);
        professional.setUserId(10L);
        professional.setDisplayName("Ana Souza");
        professional.setDocument("52998224725");
        return professional;
    }

    private static String validStateFor(Long professionalId, String nonce) {
        return professionalId + ":" + nonce + ":" + signature(professionalId.toString(), nonce);
    }

    private static String signature(String professionalId, String nonce) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(STATE_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal((professionalId + ":" + nonce).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao gerar assinatura de teste.", exception);
        }
    }
}
