package br.com.iforce.praxis.media.service;

import br.com.iforce.praxis.config.ObjectStorageProperties;
import br.com.iforce.praxis.media.dto.MediaUploadResponse;
import br.com.iforce.praxis.shared.model.MediaType;
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

    private static final Map<String, String> IMAGE_EXTENSIONS = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/jpg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private static final Map<String, String> AUDIO_EXTENSIONS = Map.of(
            "audio/mpeg", ".mp3",
            "audio/mp3", ".mp3",
            "audio/wav", ".wav",
            "audio/x-wav", ".wav",
            "audio/ogg", ".ogg",
            "audio/webm", ".webm",
            "audio/mp4", ".m4a",
            "audio/aac", ".aac"
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

        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT).trim();
        MediaType mediaType = resolveMediaType(contentType);
        String extension = extensionFor(mediaType, contentType);
        String key = "media/" + UUID.randomUUID() + extension;

        ensureBucket();
        try {
            client().putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado.", exception);
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Falha ao enviar a mídia para o armazenamento.",
                    exception
            );
        }

        return new MediaUploadResponse(publicUrl(key), mediaType, contentType, file.getSize());
    }

    private MediaType resolveMediaType(String contentType) {
        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }
        if (contentType.startsWith("audio/")) {
            return MediaType.AUDIO;
        }
        throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Apenas imagens ou áudios são suportados."
        );
    }

    private String extensionFor(MediaType mediaType, String contentType) {
        Map<String, String> extensions = mediaType == MediaType.IMAGE ? IMAGE_EXTENSIONS : AUDIO_EXTENSIONS;
        return extensions.getOrDefault(contentType, mediaType == MediaType.IMAGE ? ".img" : ".bin");
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
