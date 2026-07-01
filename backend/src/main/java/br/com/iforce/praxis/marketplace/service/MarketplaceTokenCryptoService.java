package br.com.iforce.praxis.marketplace.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
/**
 * Protege os tokens sensíveis usados na integração financeira do marketplace.
 *
 * <p>Na visão do processo, este serviço existe para que o sistema possa guardar credenciais
 * operacionais do Mercado Pago sem expor o valor original em banco ou respostas de API.</p>
 */
public class MarketplaceTokenCryptoService {

    private static final String PREFIX = "v1:";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec keySpec;

    public MarketplaceTokenCryptoService(
            @Value("${praxis.marketplace.token-encryption-secret:}") String secret
    ) {
        if (secret == null || secret.isBlank() || secret.length() < 32) {
            this.keySpec = null;
        } else {
            this.keySpec = new SecretKeySpec(sha256(secret), "AES");
        }
    }

    /**
     * Converte um token legível em uma forma segura para armazenamento.
     *
     * <p>Esse passo é usado quando a plataforma recebe ou atualiza uma credencial que precisará
     * ser reutilizada mais tarde para operar pagamentos em nome do profissional.</p>
     */
    public String encrypt(String plainText) {
        requireConfigured();
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
                    + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao criptografar token.");
        }
    }

    /**
     * Recupera o valor original de um token protegido para uso operacional controlado.
     *
     * <p>Esse fluxo ocorre apenas no backend, quando a plataforma precisa efetivamente chamar
     * o provedor financeiro com a credencial previamente guardada.</p>
     */
    public String decrypt(String encryptedValue) {
        requireConfigured();
        if (encryptedValue == null || !encryptedValue.startsWith(PREFIX)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Token Mercado Pago invalido.");
        }
        try {
            String[] parts = encryptedValue.substring(PREFIX.length()).split(":", 2);
            byte[] iv = Base64.getUrlDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getUrlDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao descriptografar token.");
        }
    }

    private void requireConfigured() {
        if (keySpec == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Segredo de criptografia do marketplace nao esta configurado."
            );
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 indisponivel.", exception);
        }
    }
}
