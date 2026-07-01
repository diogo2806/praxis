package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.billing.service.MercadoPagoClient;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
/**
 * Gerencia a conex&atilde;o OAuth entre o profissional e a conta do Mercado Pago.
 *
 * <p>No processo de neg&oacute;cio, este servi&ccedil;o &eacute; o elo que autoriza a plataforma a receber
 * pagamentos e preparar repasses em nome do vendedor dentro do marketplace.</p>
 */
public class MercadoPagoConnectService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final MercadoPagoClient mercadoPagoClient;
    private final MarketplaceProfessionalRepository professionalRepository;
    private final MarketplaceTokenCryptoService tokenCryptoService;
    private final String stateSecret;

    public MercadoPagoConnectService(
            MercadoPagoClient mercadoPagoClient,
            MarketplaceProfessionalRepository professionalRepository,
            MarketplaceTokenCryptoService tokenCryptoService,
            @Value("${praxis.marketplace.token-encryption-secret:}") String stateSecret
    ) {
        this.mercadoPagoClient = mercadoPagoClient;
        this.professionalRepository = professionalRepository;
        this.tokenCryptoService = tokenCryptoService;
        this.stateSecret = stateSecret == null ? "" : stateSecret;
    }

    @Transactional(readOnly = true)
    /**
     * Gera a URL que o profissional deve acessar para autorizar a integra&ccedil;&atilde;o com o Mercado Pago.
     *
     * <p>Esse passo inicia a etapa em que o vendedor concede permiss&atilde;o para a plataforma operar
     * o fluxo comercial do marketplace com a sua conta recebedora.</p>
     */
    public String connectUrl(String userId) {
        MarketplaceProfessionalEntity professional = loadProfessional(userId);
        return mercadoPagoClient.connectAuthorizationUrl(state(professional.getId()));
    }

    @Transactional
    /**
     * Finaliza o retorno do OAuth e guarda a credencial recebida do Mercado Pago.
     *
     * <p>Na pr&aacute;tica, este m&eacute;todo confirma que o retorno pertence ao profissional correto e
     * registra os dados necess&aacute;rios para que ele possa vender pelo marketplace.</p>
     */
    public void handleCallback(String state, String code) {
        if (code == null || code.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code Mercado Pago obrigatorio.");
        }
        MarketplaceProfessionalEntity professional = loadProfessionalByState(state);
        JsonNode tokenResponse = mercadoPagoClient.exchangeAuthorizationCode(code);
        String accessToken = text(tokenResponse, "access_token");
        if (accessToken == null || accessToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Mercado Pago nao retornou access_token.");
        }
        String sellerId = firstNonBlank(text(tokenResponse, "user_id"), text(tokenResponse, "collector_id"));
        professional.setMpSellerId(sellerId == null ? professional.getId().toString() : sellerId);
        professional.setMpAccessToken(tokenCryptoService.encrypt(accessToken));
    }

    @Transactional
    /**
     * Desfaz a liga&ccedil;&atilde;o entre o profissional e o Mercado Pago no cadastro local.
     *
     * <p>Depois deste passo, novas vendas deixam de ser eleg&iacute;veis at&eacute; que a integra&ccedil;&atilde;o seja
     * conectada novamente.</p>
     */
    public void disconnect(String userId) {
        MarketplaceProfessionalEntity professional = loadProfessional(userId);
        professional.setMpSellerId(null);
        professional.setMpAccessToken(null);
    }

    private MarketplaceProfessionalEntity loadProfessional(String userId) {
        Long id;
        try {
            id = Long.valueOf(userId);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessao invalida.");
        }
        return professionalRepository.findByUserId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado."));
    }

    private MarketplaceProfessionalEntity loadProfessionalByState(String state) {
        String[] parts = state == null ? new String[0] : state.split(":", 3);
        if (parts.length != 3 || !constantTimeEquals(signature(parts[0], parts[1]), parts[2])) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State Mercado Pago invalido.");
        }
        Long professionalId;
        try {
            professionalId = Long.valueOf(parts[0]);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State Mercado Pago invalido.");
        }
        return professionalRepository.findById(professionalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado."));
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String state(Long professionalId) {
        byte[] random = new byte[16];
        secureRandom.nextBytes(random);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        return professionalId + ":" + nonce + ":" + signature(professionalId.toString(), nonce);
    }

    private String signature(String professionalId, String nonce) {
        if (stateSecret.isBlank() || stateSecret.length() < 32) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Segredo de state Mercado Pago nao esta configurado."
            );
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stateSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal((professionalId + ":" + nonce).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao assinar state Mercado Pago.");
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
