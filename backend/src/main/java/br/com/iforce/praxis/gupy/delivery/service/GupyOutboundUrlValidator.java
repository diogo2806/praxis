package br.com.iforce.praxis.gupy.delivery.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

@Component
public class GupyOutboundUrlValidator {

    private final boolean securityEnabled;

    public GupyOutboundUrlValidator(
            @Value("${praxis.security.enabled:true}") boolean securityEnabled
    ) {
        this.securityEnabled = securityEnabled;
    }

    public URI validate(String outboundUrl) {
        URI uri = URI.create(outboundUrl);
        validateForPersistence(uri);
        assertPublicAddress(normalizeHost(uri.getHost()));
        return uri;
    }

    /**
     * Valida o contrato da URL antes de persistir a tentativa, sem depender de DNS externo.
     * A resolução completa e a proteção contra DNS rebinding continuam sendo executadas
     * imediatamente antes da chamada HTTP em {@link #validate(String)}.
     */
    public void validateForPersistence(URI uri) {
        if (uri == null || !uri.isAbsolute() || uri.getHost() == null) {
            throw new IllegalArgumentException("URL externa deve ser absoluta e possuir host.");
        }

        String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        String host = normalizeHost(uri.getHost());

        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new IllegalArgumentException("URL externa deve usar HTTP ou HTTPS.");
        }
        if (securityEnabled && !scheme.equals("https")) {
            throw new IllegalArgumentException("URL externa deve usar HTTPS em produção.");
        }
        if (host.isBlank() || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("URL externa inválida.");
        }

        assertPublicLiteralAddress(host);
    }

    private void assertPublicLiteralAddress(String host) {
        if (!isIpLiteral(host)) {
            return;
        }
        try {
            InetAddress address = InetAddress.getByName(host);
            if (isForbiddenAddress(address)) {
                throw new IllegalArgumentException("URL externa não pode apontar para rede local ou reservada.");
            }
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Endereço IP externo inválido.", exception);
        }
    }

    private void assertPublicAddress(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isForbiddenAddress(address)) {
                    throw new IllegalArgumentException("URL externa não pode apontar para rede local ou reservada.");
                }
            }
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Host externo não resolvido.", exception);
        }
    }

    private boolean isIpLiteral(String host) {
        return host.contains(":") || host.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
    }

    private String normalizeHost(String host) {
        String normalized = host == null ? "" : host.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("[") && normalized.endsWith("]")) {
            return normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private boolean isForbiddenAddress(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isUniqueLocalIpv6(address);
    }

    private boolean isUniqueLocalIpv6(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        byte firstByte = address.getAddress()[0];
        return (firstByte & 0xfe) == 0xfc;
    }
}
