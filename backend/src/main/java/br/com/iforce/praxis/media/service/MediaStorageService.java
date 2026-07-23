package br.com.iforce.praxis.media.service;

import br.com.iforce.praxis.auth.context.EmpresaContextHolder;

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
 * Cuida do recebimento, validação e armazenamento das mídias das avaliações.
 *
 * <p>Na visão do processo, este serviço garante que imagens, áudios e vídeos anexados a
 * uma avaliação passem por uma conferência mínima antes de entrarem no produto.
 * Ele confirma se o storage está configurado, impede arquivos vazios ou grandes
 * demais, identifica se o conteúdo é realmente imagem, áudio ou vídeo, grava em uma
 * área separada por empresa e devolve a URL que a tela pode salvar no roteiro.</p>
 */
@Service
public class MediaStorageService {

    private static final long MAX_STANDARD_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_FILE_SIZE_BYTES = 100L * 1024 * 1024;
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

    private static final Map<String, String> VIDEO_EXTENSIONS = Map.ofEntries(
            Map.entry("video/mp4", ".mp4"),
            Map.entry("video/webm", ".webm"),
            Map.entry("video/ogg", ".ogv"),
            Map.entry("video/quicktime", ".mov")
    );

    private final ObjectStorageProperties properties;
    private volatile S3Client s3Client;
    private volatile boolean bucketReady;

    /**
     * Monta o serviço com as configurações de armazenamento do ambiente.
     *
     * <p>Para a operação, essas configurações indicam onde as mídias serão
     * guardadas, qual bucket será usado e qual endereço público será devolvido
     * para que a tela consiga exibir ou reutilizar o arquivo depois do upload.</p>
     *
     * @param properties configurações de endpoint, credenciais, bucket e URL pública do storage
     */
    public MediaStorageService(ObjectStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * Recebe, valida e armazena uma mídia enviada para uma avaliação.
     *
     * <p>Fluxo do processo: primeiro verifica se o armazenamento de mídia está
     * disponível. Depois confere se o arquivo veio preenchido e se respeita o
     * limite de tamanho. Em seguida, identifica o tipo real do conteúdo para
     * aceitar apenas imagem, áudio ou vídeo. Por fim, grava o arquivo em uma pasta da
     * empresa atual e devolve a URL pública para uso no cadastro da avaliação.</p>
     *
     * @param file imagem, áudio ou vídeo enviado pela tela de cadastro
     * @return endereço público da mídia, tipo identificado, formato do arquivo e tamanho armazenado
     */
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
        if (file.getSize() > MAX_VIDEO_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Mídia excede o tamanho máximo de 100MB.");
        }

        byte[] bytes = readBytes(file);
        String detectedContentType = normalizeContentType(TIKA.detect(bytes));
        MediaType mediaType = resolveMediaType(detectedContentType);
        if (mediaType != MediaType.VIDEO && bytes.length > MAX_STANDARD_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Imagem ou áudio excede o tamanho máximo de 10MB.");
        }
        String extension = extensionFor(mediaType, detectedContentType);

        // Isola a mídia por empresa: permite limpeza no offboarding (deletar o prefixo
        // media/{empresaId}/) e serve de base para cota/limite por plano.
        String empresaId = EmpresaContextHolder.getRequired();
        String key = "media/" + empresaId + "/" + UUID.randomUUID() + extension;

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

    /**
     * Transforma o arquivo recebido pela tela em bytes para validação e armazenamento.
     *
     * <p>No processo de upload, este passo é a leitura segura do anexo. Se o
     * sistema não conseguir abrir o arquivo enviado, a operação é recusada antes
     * de qualquer tentativa de gravação no storage.</p>
     *
     * @param file arquivo recebido no formulário de upload
     * @return conteúdo binário que será inspecionado e armazenado
     */
    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível ler o arquivo enviado.", exception);
        }
    }

    /**
     * Decide se o conteúdo enviado é uma imagem ou um áudio aceito pelo Praxis.
     *
     * <p>Este método protege o processo de cadastro impedindo que arquivos de
     * outros formatos entrem como mídia de avaliação. A decisão é feita pelo tipo
     * real detectado no conteúdo, não apenas pelo nome informado pelo usuário.</p>
     *
     * @param contentType formato detectado no conteúdo do arquivo
     * @return categoria de mídia usada pela avaliação
     */
    private MediaType resolveMediaType(String contentType) {
        if (IMAGE_EXTENSIONS.containsKey(contentType)) {
            return MediaType.IMAGE;
        }
        if (AUDIO_EXTENSIONS.containsKey(contentType)) {
            return MediaType.AUDIO;
        }
        if (VIDEO_EXTENSIONS.containsKey(contentType)) {
            return MediaType.VIDEO;
        }
        throw new ResponseStatusException(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Apenas imagens, áudios ou vídeos nos formatos permitidos são suportados."
        );
    }

    /**
     * Escolhe a extensão final do arquivo a partir do tipo validado.
     *
     * <p>Depois que o sistema confirma que a mídia é permitida, este método
     * define a extensão usada no nome gravado no storage. Isso mantém o arquivo
     * coerente com o formato detectado e facilita sua leitura posterior pelo
     * navegador ou por ferramentas de acessibilidade.</p>
     *
     * @param mediaType categoria de mídia validada para o arquivo
     * @param contentType formato específico detectado no conteúdo
     * @return extensão usada no nome do arquivo armazenado
     */
    private String extensionFor(MediaType mediaType, String contentType) {
        return switch (mediaType) {
            case IMAGE -> IMAGE_EXTENSIONS.get(contentType);
            case AUDIO -> AUDIO_EXTENSIONS.get(contentType);
            case VIDEO -> VIDEO_EXTENSIONS.get(contentType);
        };
    }

    /**
     * Padroniza o tipo de conteúdo para comparação com a lista de formatos aceitos.
     *
     * <p>Alguns arquivos podem chegar com letras maiúsculas ou parâmetros extras
     * no tipo de conteúdo. Este método limpa essa informação para que o processo
     * de validação compare sempre valores simples, como {@code image/png} ou
     * {@code audio/mpeg}.</p>
     *
     * @param contentType tipo de conteúdo detectado antes da padronização
     * @return tipo de conteúdo em formato simples, minúsculo e sem parâmetros extras
     */
    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int parametersStart = contentType.indexOf(';');
        String value = parametersStart >= 0 ? contentType.substring(0, parametersStart) : contentType;
        return value.toLowerCase(Locale.ROOT).trim();
    }

    /**
     * Monta o endereço público que será salvo e exibido pela tela.
     *
     * <p>Depois que o arquivo é armazenado, a pessoa usuária não precisa saber a
     * chave interna do storage. Este método converte essa chave na URL que o
     * frontend pode guardar no cenário, na alternativa ou na descrição acessível.</p>
     *
     * @param key caminho interno do arquivo no storage
     * @return URL pública usada para acessar a mídia armazenada
     */
    private String publicUrl(String key) {
        String base = properties.effectivePublicUrl();
        if (properties.pathStyle()) {
            return base + "/" + properties.bucket() + "/" + key;
        }
        return base + "/" + key;
    }

    /**
     * Garante que o local de armazenamento exista antes do primeiro upload.
     *
     * <p>Na operação, evita que o usuário receba erro porque o bucket ainda não
     * foi criado. O método confere uma vez se o bucket está disponível e, se não
     * existir, cria o espaço necessário para que as mídias possam ser gravadas.</p>
     */
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

    /**
     * Entrega o cliente usado para conversar com o storage compatível com S3.
     *
     * <p>Este método prepara a conexão com o armazenamento usando as credenciais
     * e o endpoint configurados. Para não recriar essa conexão a cada upload, o
     * cliente é montado uma vez e reaproveitado nas próximas mídias enviadas.</p>
     *
     * @return cliente de comunicação com o storage de mídia
     */
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
