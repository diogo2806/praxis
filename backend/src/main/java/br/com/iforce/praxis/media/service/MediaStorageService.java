package br.com.iforce.praxis.media.service;

import br.com.iforce.praxis.auth.context.TenantContextHolder;
import br.com.iforce.praxis.config.ObjectStorageProperties;
import br.com.iforce.praxis.media.dto.MediaUploadResponse;
import br.com.iforce.praxis.shared.model.MediaType;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Faz upload das mídias (imagem/áudio) usadas no cadastro dos testes para o storage S3 compatível
 * (MinIO) e devolve a URL pública persistida junto ao turno ou à alternativa.
 */
@Service
public class MediaStorageService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Tika TIKA = new Tika();

    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private static final Map<String, String> AUDIO_EXTENSIONS = Map.ofEntries(
            Map.entry("audio/mpeg", ".mp3"),
            Map.entry("audio/mp3", ".mp3"),
            Map.entry("audio/wav", ".wav"),
            Map.entry("audio/x-wav", ".wav"),
            Map.entry("audio/vnd.wave", ".wav"),
            Map.entry("audio/ogg", ".ogg"),
            Map.entry("audio/webm", ".webm"),
            Map.entry("audio/mp4", ".m4a"),
            Map.entry("audio/x-m4a", ".m4a"),
            Map.entry("audio/aac", ".aac")
    );

    private final ObjectStorageProperties properties;
    private volatile S3Client s3Client;
    private volatile boolean bucketReady;

    public MediaStorageService(ObjectStorageProperties properties) {
        this.properties = properties;
    }

    public MediaUploadResponse upload(MultipartFile file) {
        if (!properties.isConfigured()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Armazenamento de mídia não está configurado (OBJECT_STORAGE_ENDPOINT)."
            );
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Arquivo de mídia vazio.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Mídia excede o tamanho máximo de 10MB.");
        }

        byte[] bytes = readBytes(file);
        String detectedContentType = normalizeContentType(TIKA.detect(bytes));
        MediaType mediaType = resolveMediaType(detectedContentType);
        String extension = extensionFor(mediaType, detectedContentType);

        // Isola a mídia por tenant: permite limpeza no offboarding (deletar o prefixo
        // media/{tenantId}/) e serve de base para cota/limite por plano.
        String tenantId = TenantContextHolder.getRequired();
        String key = "media/" + tenantId + "/" + UUID.randomUUID() + extension;

        ensureBucket();
        try {
            client().putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(key)
                            .contentType(detectedContentType)
                            .build(),
                    RequestBody.fromBytes(bytes)
            );
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Falha ao enviar a mídia para o armazenamento.",
                    exception
            );
        }

        return new MediaUploadResponse(publicUrl(key), mediaType, detectedContentType, bytes.length);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado.", exception);
        }
    }

    private MediaType resolveMediaType(String contentType) {
        if (IMAGE_EXTENSIONS.containsKey(contentType)) {
            return MediaType.IMAGE;
        }
        if (AUDIO_EXTENSIONS.containsKey(contentType)) {
            return MediaType.AUDIO;
        }
        throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Apenas imagens ou áudios são suportados."
        );
    }

    private String extensionFor(MediaType mediaType, String contentType) {
        Map<String, String> extensions = mediaType == MediaType.IMAGE ? IMAGE_EXTENSIONS : AUDIO_EXTENSIONS;
        return extensions.get(contentType);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parametersStart = contentType.indexOf(';');
        String value = parametersStart >= 0 ? contentType.substring(0, parametersStart) : contentType;
        return value.toLowerCase(Locale.ROOT).trim();
    }

    private String publicUrl(String key) {
        String base = properties.effectivePublicUrl();
        if (properties.pathStyle()) {
            return base + "/" + properties.bucket() + "/" + key;
        }
        return base + "/" + key;
    }

    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            try {
                client().headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
            } catch (NoSuchBucketException notFound) {
                client().createBucket(builder -> builder.bucket(properties.bucket()));
            }
            bucketReady = true;
        }
    }

    private S3Client client() {
        S3Client local = s3Client;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (s3Client == null) {
                s3Client = S3Client.builder()
                        .endpointOverride(URI.create(properties.endpoint()))
                        .region(Region.of(properties.region()))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                        .serviceConfiguration(S3Configuration.builder()
                                .pathStyleAccessEnabled(properties.pathStyle())
                                .build())
                        .build();
            }
            return s3Client;
        }
    }
}
