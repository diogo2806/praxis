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
    private final HostResolver hostResolver;

    public GupyOutboundUrlValidator(
            @Value("${praxis.security.enabled:true}") boolean securityEnabled
    ) {
        this(securityEnabled, InetAddress::getAllByName);
    }

    GupyOutboundUrlValidator(boolean securityEnabled, HostResolver hostResolver) {
        this.securityEnabled = securityEnabled;
        this.hostResolver = hostResolver;
    }

    public URI validate(String outboundUrl) {
        return validateAndResolve(outboundUrl).uri();
    }

    public ValidatedOutboundTarget validateAndResolve(String outboundUrl) {
        URI uri = URI.create(outboundUrl);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);

        if (!scheme.equals("https") && !scheme.equals("http")) {
            throw new IllegalArgumentException("URL externa deve usar HTTP ou HTTPS.");
        }
        if (securityEnabled && !scheme.equals("https")) {
            throw new IllegalArgumentException("URL externa deve usar HTTPS em produção.");
        }
        if (host.isBlank() || uri.getUserInfo() != null) {
            throw new IllegalArgumentException("URL externa inválida.");
        }

        InetAddress[] addresses = resolvePublicAddresses(host);
        return new ValidatedOutboundTarget(uri, addresses);
    }

    private InetAddress[] resolvePublicAddresses(String host) {
        try {
            InetAddress[] addresses = hostResolver.resolve(host);
            if (addresses.length == 0) {
                throw new IllegalArgumentException("Host externo não resolvido.");
            }
            for (InetAddress address : addresses) {
                if (isForbiddenAddress(address)) {
                    throw new IllegalArgumentException("URL externa não pode apontar para rede local ou reservada.");
                }
            }
            return addresses;
        } catch (UnknownHostException exception) {
            throw new IllegalArgumentException("Host externo não resolvido.", exception);
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

    @FunctionalInterface
    interface HostResolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }
}
