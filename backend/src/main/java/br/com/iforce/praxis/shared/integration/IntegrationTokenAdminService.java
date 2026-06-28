package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.service.CurrentTenantService;
import br.com.iforce.praxis.shared.integration.dto.IntegrationTokenResponse;
import br.com.iforce.praxis.shared.integration.dto.RotateIntegrationTokenResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Gerencia os tokens de segurança para integração com sistemas externos.
 *
 * Empresas que desejam integrar com a Praxis (por exemplo, para sincronizar dados com Gupy)
 * precisam de um token de segurança único. Este serviço permite criar, listar e renovar esses tokens.
 *
 * Cada token é único por empresa e plataforma de integração. O token é guardado de forma
 * encriptada: apenas o hash dele é armazenado, nunca o valor real. Assim, mesmo que alguém
 * acesse o banco de dados, não consegue usar os tokens.
 *
 * Quando um token é comprometido (alguém descobriu), pode ser renovado para gerar um novo,
 * invalidando o anterior imediatamente. Também pode ser revogado totalmente.
 */
@Service
public class IntegrationTokenAdminService {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("gupy", "recrutei");
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final CurrentTenantService currentTenantService;
    private final TenantRepository tenantRepository;
    private final IntegrationTokenRepository integrationTokenRepository;

    public IntegrationTokenAdminService(
            CurrentTenantService currentTenantService,
            TenantRepository tenantRepository,
            IntegrationTokenRepository integrationTokenRepository
    ) {
        this.currentTenantService = currentTenantService;
        this.tenantRepository = tenantRepository;
        this.integrationTokenRepository = integrationTokenRepository;
    }

    /**
     * Lista todos os tokens de integração da empresa atual.
     *
     * Retorna um resumo de cada integração suportada mostrando:
     * - Se existe um token ativo para aquela integração
     * - Quando o token foi criado (para controle de validade)
     *
     * Não expõe o token em si, apenas seu status e data. Os valores dos tokens
     * nunca são retornados à interface, por questões de segurança.
     *
     * @return Lista com status de cada integração disponível (Gupy, Recrutei, etc)
     */
    @Transactional(readOnly = true)
    public List<IntegrationTokenResponse> listTokens() {
        String tenantId = currentTenantService.requiredTenantId();
        Map<String, IntegrationTokenEntity> existing = integrationTokenRepository
                .findByTenantIdOrderByProviderAsc(tenantId)
                .stream()
                .collect(Collectors.toMap(IntegrationTokenEntity::getProvider, token -> token, (left, right) -> left));

        return SUPPORTED_PROVIDERS.stream()
                .sorted()
                .map(provider -> {
                    IntegrationTokenEntity token = existing.get(provider);
                    return new IntegrationTokenResponse(provider, token != null, token == null ? null : token.getCreatedAt());
                })
                .toList();
    }

    /**
     * Gera um novo token para uma integração, invalidando o anterior.
     *
     * Usado quando:
     * - É a primeira vez que a empresa vai usar uma integração (criar token novo)
     * - O token foi comprometido e precisa ser substituído (renovar por segurança)
     *
     * O novo token é retornado apenas uma única vez. O sistema guarda apenas o hash
     * (transformação matemática) do token, não seu valor real. Isso significa que o
     * token nunca pode ser recuperado se a empresa perder - precisará gerar um novo.
     *
     * O token antigo é imediatamente descartado e não funciona mais para autenticar.
     *
     * @param provider Nome da plataforma (Gupy, Recrutei, etc)
     * @return O novo token gerado, que deve ser salvo com cuidado. Não será retornado novamente.
     * @throws ResponseStatusException se a plataforma não é suportada
     */
    @Transactional
    public RotateIntegrationTokenResponse rotateToken(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        String tenantId = currentTenantService.requiredTenantId();
        TenantEntity tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Não encontramos os dados da sua empresa."));

        String tokenValue = generateToken();
        integrationTokenRepository.deleteByTenantIdAndProvider(tenantId, normalizedProvider);
        integrationTokenRepository.flush();

        IntegrationTokenEntity entity = new IntegrationTokenEntity();
        entity.setTenant(tenant);
        entity.setProvider(normalizedProvider);
        entity.setTokenHash(sha256(tokenValue));
        entity.setCreatedAt(Instant.now());

        IntegrationTokenEntity saved = integrationTokenRepository.save(entity);
        return new RotateIntegrationTokenResponse(
                saved.getProvider(),
                true,
                saved.getCreatedAt(),
                tokenValue
        );
    }

    /**
     * Remove completamente o token de uma integração.
     *
     * Usa-se isso quando a empresa não quer mais usar uma integração específica.
     * Qualquer tentativa posterior de usar aquela integração será rejeitada até
     * que um novo token seja gerado.
     *
     * @param provider Nome da plataforma (Gupy, Recrutei, etc)
     * @throws ResponseStatusException se a plataforma não é suportada
     */
    @Transactional
    public void revokeToken(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        String tenantId = currentTenantService.requiredTenantId();
        integrationTokenRepository.deleteByTenantIdAndProvider(tenantId, normalizedProvider);
    }

    /**
     * Padroniza o nome da plataforma de integração.
     *
     * Converte para minúsculas, remove espaços extras, e valida se a plataforma
     * é suportada. Ajuda a evitar erros por digitação.
     *
     * @param provider Nome da plataforma fornecido pelo usuário
     * @return Nome padronizado da plataforma
     * @throws ResponseStatusException se a plataforma não é suportada
     */
    private static String normalizeProvider(String provider) {
        String normalized = provider == null ? "" : provider.trim().toLowerCase();
        if (!SUPPORTED_PROVIDERS.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provedor de integração não suportado.");
        }
        return normalized;
    }

    /**
     * Gera um token seguro e aleatório.
     *
     * Cria uma sequência aleatória de 32 bytes usando um gerador criptograficamente seguro,
     * então a codifica em um formato texto. O prefixo "prx_" ajuda a identificar que é
     * um token da Praxis.
     *
     * Exemplo: prx_aB12xYzqWpLmNoPqRs...
     *
     * @return Um novo token único nunca visto antes
     */
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return "prx_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Cria um hash criptográfico (impressão digital) do token.
     *
     * Usa o algoritmo SHA-256 para transformar o token em um código único.
     * É uma transformação irreversível: dado o hash, é impossível descobrir o token original.
     *
     * Armazenar o hash em vez do token em si garante que mesmo se alguém roubar o banco de dados,
     * não consegue usar os tokens. Quando o sistema recebe um token em uma requisição, faz o
     * mesmo hash e compara com o valor armazenado.
     *
     * @param value O token a ser transformado
     * @return O hash do token
     */
    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
