package br.com.iforce.praxis.auth.service;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

/**
 * Resolve a identidade de rede usada em auditoria e rate limit.
 *
 * <p>Os cabeçalhos encaminhados são interpretados exclusivamente pelo container, que aplica a
 * lista de proxies confiáveis. A aplicação nunca lê {@code X-Forwarded-For} diretamente.</p>
 */
@Component
public class ClientIpResolver {

    private static final int MAX_ADDRESS_LENGTH = 64;
    private static final String UNKNOWN_ADDRESS = "unknown";

    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return UNKNOWN_ADDRESS;
        }

        String normalizedAddress = remoteAddress.trim();
        if (normalizedAddress.length() > MAX_ADDRESS_LENGTH) {
            return UNKNOWN_ADDRESS;
        }

        return normalizedAddress;
    }
}
