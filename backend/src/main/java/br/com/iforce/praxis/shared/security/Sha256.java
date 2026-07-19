package br.com.iforce.praxis.shared.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Operações SHA-256 compartilhadas pelos fluxos de autenticação, tokens e
 * identificadores opacos. Centralizar o algoritmo evita diferenças silenciosas
 * de charset, Base64 e representação hexadecimal entre módulos.
 */
public final class Sha256 {

    private Sha256() {
    }

    public static byte[] digest(String value) {
        Objects.requireNonNull(value, "Valor obrigatório para cálculo de SHA-256.");
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    public static String hex(String value) {
        return HexFormat.of().formatHex(digest(value));
    }

    public static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest(value));
    }

    public static String hexPrefix(String value, int byteCount) {
        byte[] hash = digest(value);
        if (byteCount <= 0 || byteCount > hash.length) {
            throw new IllegalArgumentException("Quantidade de bytes do prefixo SHA-256 inválida.");
        }
        return HexFormat.of().formatHex(hash, 0, byteCount);
    }
}
