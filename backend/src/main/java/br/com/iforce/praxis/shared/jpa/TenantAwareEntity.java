package br.com.iforce.praxis.shared.jpa;

public interface TenantAwareEntity {
    String getTenantId();
}
