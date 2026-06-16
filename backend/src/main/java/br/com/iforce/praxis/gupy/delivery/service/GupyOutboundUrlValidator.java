package br.com.iforce.praxis.gupy.delivery.service;

import br.com.iforce.praxis.config.PraxisProperties;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;

@Component
public class GupyOutboundUrlValidator {

    private final PraxisProperties praxisProperties;

    public GupyOutboundUrlValidator(PraxisProperties praxisProperties) {
        this.praxisProperties = praxisProperties;
    }

    public URI validate(String outboundUrl) {
        URI uri = URI.create(outboundUrl);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new IllegalArgumentException("URL externa deve usar HTTP ou HTTPS.");
        }
        if (host.isBlank() || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL externa invalida.");
        }
        if (!isAllowedHost(host)) {
            throw new IllegalArgumentException("Host externo nao permitido.");
        }
        assertPublicAddress(host);
        return uri;
    }

    private boolean isAllowedHost(String host) {
        return praxisProperties.webhookAllowedHosts().stream()
                .map(allowedHost -> allowedHost == null ? "" : allowedHost.trim().toLowerCase(Locale.ROOT))
                .filter(allowedHost -> !allowedHost.isBlank())
                .anyMatch(allowedHost -> host.equals(allowedHost) || host.endsWith("." + allowedHost));
    }

    private void assertPublicAddress(String host) {
        try {
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (isForbiddenAddress(address)) {
                    throw new IllegalArgumentException("URL externa nao pode apontar para rede local ou reservada.");
                }
            }
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Host externo nao resolvido.", exception);
        }
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
