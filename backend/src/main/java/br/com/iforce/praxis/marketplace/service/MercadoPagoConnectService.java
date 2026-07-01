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
import java.security.SecureRandom;
import java.util.Base64;

@Service
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
    public String connectUrl(String userId) {
        MarketplaceProfessionalEntity professional = loadProfessional(userId);
        return mercadoPagoClient.connectAuthorizationUrl(state(professional.getId()));
    }

    @Transactional
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
        if (parts.length != 3 || !signature(parts[0], parts[1]).equals(parts[2])) {
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
            mac.init(new SecretKeySpec(stateSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal((professionalId + ":" + nonce).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
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
