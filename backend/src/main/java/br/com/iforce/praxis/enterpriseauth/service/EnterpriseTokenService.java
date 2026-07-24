package br.com.iforce.praxis.enterpriseauth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EnterpriseTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_TTL_SECONDS = 3600;

    private final ObjectMapper objectMapper;
    private final byte[] signingSecret;

    public EnterpriseTokenService(
            ObjectMapper objectMapper,
            @Value("${praxis.enterprise-auth.signing-secret:${PRAXIS_ENTERPRISE_AUTH_SIGNING_SECRET:}}")
            String signingSecret
    ) {
        this.objectMapper = objectMapper;
        this.signingSecret = signingSecret == null
                ? new byte[0]
                : signingSecret.getBytes(StandardCharsets.UTF_8);
    }

    public IssuedToken issue(
            String subject,
            String email,
            String displayName,
            String empresaId,
            List<String> roles,
            boolean mfaVerified,
            String providerId
    ) {
        requireConfigured();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(TOKEN_TTL_SECONDS);
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "PRAXIS-ENTERPRISE");
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", "praxis-enterprise-auth");
        claims.put("sub", subject);
        claims.put("email", email);
        claims.put("name", displayName);
        claims.put("empresaId", empresaId);
        claims.put("roles", roles);
        claims.put("mfa", mfaVerified);
        claims.put("providerId", providerId);
        claims.put("authSource", "ENTERPRISE_SSO");
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());
        String unsigned = base64Json(header) + "." + base64Json(claims);
        String token = unsigned + "." + base64Url(hmac(unsigned.getBytes(StandardCharsets.UTF_8)));
        return new IssuedToken(token, expiresAt, TOKEN_TTL_SECONDS);
    }

    public EnterprisePrincipal verify(String token) {
        requireConfigured();
        String[] parts = token == null ? new String[0] : token.split("\\.");
        if (parts.length != 3) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token corporativo inválido.");
        }
        String unsigned = parts[0] + "." + parts[1];
        byte[] expected = hmac(unsigned.getBytes(StandardCharsets.UTF_8));
        byte[] received;
        try {
            received = Base64.getUrlDecoder().decode(parts[2]);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token corporativo inválido.");
        }
        if (!MessageDigest.isEqual(expected, received)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Assinatura do token corporativo inválida.");
        }
        try {
            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!"PRAXIS-ENTERPRISE".equals(header.path("typ").asText())
                    || !"praxis-enterprise-auth".equals(claims.path("iss").asText())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Emissor do token inválido.");
            }
            long expiresAt = claims.path("exp").asLong(0);
            if (expiresAt <= Instant.now().getEpochSecond()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token corporativo expirado.");
            }
            List<String> roles = objectMapper.convertValue(
                    claims.path("roles"),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
            );
            return new EnterprisePrincipal(
                    claims.path("sub").asText(),
                    claims.path("email").asText(),
                    claims.path("name").asText(),
                    claims.path("empresaId").asText(),
                    roles,
                    claims.path("mfa").asBoolean(false),
                    claims.path("providerId").asText()
            );
        } catch (IOException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Conteúdo do token corporativo inválido.");
        }
    }

    public boolean isEnterpriseToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        try {
            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            return "PRAXIS-ENTERPRISE".equals(header.path("typ").asText());
        } catch (Exception exception) {
            return false;
        }
    }

    private String base64Json(Object value) {
        try {
            return base64Url(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível gerar o token corporativo.", exception);
        }
    }

    private byte[] hmac(byte[] value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(signingSecret, HMAC_ALGORITHM));
            return mac.doFinal(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível assinar o token corporativo.", exception);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private void requireConfigured() {
        if (signingSecret.length < 32) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configure PRAXIS_ENTERPRISE_AUTH_SIGNING_SECRET com pelo menos 32 caracteres."
            );
        }
    }

    public record IssuedToken(String value, Instant expiresAt, long expiresInSeconds) {
    }

    public record EnterprisePrincipal(
            String subject,
            String email,
            String displayName,
            String empresaId,
            List<String> roles,
            boolean mfaVerified,
            String providerId
    ) {
    }
}
