package br.com.iforce.praxis.gupy.delivery.service;

import java.net.InetAddress;
import java.net.URI;
import java.util.Arrays;

public record ValidatedOutboundTarget(URI uri, InetAddress[] addresses) {

    public ValidatedOutboundTarget {
        addresses = Arrays.copyOf(addresses, addresses.length);
    }

    @Override
    public InetAddress[] addresses() {
        return Arrays.copyOf(addresses, addresses.length);
    }
}
