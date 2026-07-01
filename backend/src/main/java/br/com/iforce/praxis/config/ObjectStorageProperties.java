package br.com.iforce.praxis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


/**
 * Configuração do armazenamento de objetos (MinIO / S3 compatível) usado para mídias dos testes.
 * Quando {@link #endpoint()} está em branco o upload de mídia fica desabilitado.
 */
@ConfigurationProperties(prefix = "praxis.object-storage")
public record ObjectStorageProperties(
        String endpoint,
        String publicUrl,
        String region,
        String accessKey,
        String secretKey,
        String bucket,
        boolean pathStyle
) {

    public ObjectStorageProperties {
        if (region == null || region.isBlank()) {
            region = "us-east-1";
        }
        if (bucket == null || bucket.isBlank()) {
            bucket = "praxis-media";
        }
    }

    public boolean isConfigured() {
        return endpoint != null && !endpoint.isBlank();
    }

    /**
     * Base pública usada pelo navegador para baixar a mídia. Quando não informada,
     * cai de volta para o próprio endpoint do storage.
     */
    public String effectivePublicUrl() {
        String base = publicUrl != null && !publicUrl.isBlank() ? publicUrl : endpoint;
        if (base == null) {
            return null;
        }
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
}
