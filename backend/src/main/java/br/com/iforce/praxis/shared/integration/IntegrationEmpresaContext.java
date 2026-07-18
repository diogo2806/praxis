package br.com.iforce.praxis.shared.integration;

public record IntegrationEmpresaContext(
        String empresaId,
        String companyId,
        String provider,
        String partnerClientId
) {

    public IntegrationEmpresaContext(String empresaId, String companyId, String provider) {
        this(empresaId, companyId, provider, null);
    }
}
