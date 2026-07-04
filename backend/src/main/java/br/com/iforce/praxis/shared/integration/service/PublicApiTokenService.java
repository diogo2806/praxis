package br.com.iforce.praxis.shared.integration.service;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.shared.integration.dto.PublicApiTokenResponse;

import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;

import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;

import br.com.iforce.praxis.shared.integration.model.IntegrationType;

import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;

import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;

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


/**
 * Cuida do token que permite ao cliente consumir a API pública da Práxis.
 *
 * <p>Na visão do processo, este serviço atende a tela de integrações quando o
 * cliente precisa gerar, trocar ou cancelar a chave de acesso usada por sistemas
 * externos. A chave completa aparece somente no momento da geração ou rotação;
 * depois disso o sistema guarda apenas uma versão protegida, evitando que alguém
 * recupere o segredo original pelo banco de dados.</p>
 *
 * <p>O token público é independente do webhook: o webhook é o canal pelo qual a
 * Práxis envia eventos para o cliente, enquanto este token é o canal pelo qual o
 * cliente consulta a Práxis.</p>
 */
@Service
public class PublicApiTokenService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOKEN_PREFIX = "prx_live_";

    private final EmpresaIntegrationRepository empresaIntegrationRepository;
    private final EmpresaRepository empresaRepository;
    private final CurrentEmpresaService currentEmpresaService;

    /**
     * Prepara o serviço com os acessos necessários para localizar a empresa
     * logada e salvar a configuração da integração pública.
     *
     * @param empresaIntegrationRepository repositório onde a configuração da integração é armazenada
     * @param empresaRepository repositório usado para confirmar a existência da empresa
     * @param currentEmpresaService serviço que identifica a empresa do usuário logado
     */
    public PublicApiTokenService(
            EmpresaIntegrationRepository empresaIntegrationRepository,
            EmpresaRepository empresaRepository,
            CurrentEmpresaService currentEmpresaService
    ) {
        this.empresaIntegrationRepository = empresaIntegrationRepository;
        this.empresaRepository = empresaRepository;
        this.currentEmpresaService = currentEmpresaService;
    }

    /**
     * Gera uma nova chave de API pública para a empresa logada.
     *
     * <p>Fluxo do processo: quando o cliente clica para gerar ou rotacionar o
     * token, o sistema cria uma chave nova, substitui qualquer chave anterior e
     * devolve o valor completo apenas nessa resposta. Para as próximas telas, o
     * sistema mostra somente uma prévia mascarada, suficiente para o cliente
     * reconhecer qual chave está ativa sem expor o segredo.</p>
     *
     * @return token completo para cópia imediata e prévia mascarada para exibição futura
     */
    @Transactional
    public PublicApiTokenResponse generateToken() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        EmpresaIntegrationEntity entity = findOrCreate(empresaId);

        String token = TOKEN_PREFIX + randomToken();
        entity.setCredentialsHash(sha256(token));
        entity.setTokenPreview(preview(token));
        entity.setUpdatedAt(Instant.now());
        empresaIntegrationRepository.save(entity);

        return new PublicApiTokenResponse(token, preview(token));
    }

    /**
     * Cancela a chave de API pública ativa da empresa logada.
     *
     * <p>Fluxo do processo: quando o cliente revoga o token, qualquer sistema
     * externo que usava aquela chave deixa de conseguir consumir a API pública.
     * A configuração de webhook permanece intacta, porque ela representa outro
     * caminho de integração.</p>
     */
    @Transactional
    public void revokeToken() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .ifPresent(entity -> {
                    entity.setCredentialsHash(null);
                    entity.setTokenPreview(null);
                    entity.setUpdatedAt(Instant.now());
                    empresaIntegrationRepository.save(entity);
                });
    }

    /**
     * Localiza o cadastro de integração pública da empresa ou cria um registro
     * inicial quando o cliente ainda não configurou esse canal.
     *
     * <p>Uso interno para garantir que a geração do token sempre tenha um lugar
     * correto para salvar a configuração, sem exigir que outro fluxo tenha sido
     * executado antes.</p>
     *
     * @param empresaId empresa dona da integração pública
     * @return configuração existente ou recém-criada para a API pública
     * @throws ResponseStatusException se a empresa informada não existir
     */
    private EmpresaIntegrationEntity findOrCreate(String empresaId) {
        return empresaIntegrationRepository.findFirstByEmpresaIdAndProvider(empresaId, IntegrationProvider.CUSTOM_API)
                .orElseGet(() -> {
                    EmpresaEntity empresa = empresaRepository.findById(empresaId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
                    EmpresaIntegrationEntity created = new EmpresaIntegrationEntity();
                    created.setEmpresa(empresa);
                    created.setProvider(IntegrationProvider.CUSTOM_API);
                    created.setType(IntegrationType.API);
                    created.setStatus(IntegrationStatus.CONECTADA);
                    created.setConfiguredAt(Instant.now());
                    created.setCreatedAt(Instant.now());
                    created.setUpdatedAt(Instant.now());
                    return created;
                });
    }

    /**
     * Cria a parte aleatória e imprevisível do token.
     *
     * <p>Uso interno para garantir que cada chave entregue ao cliente seja única
     * e difícil de adivinhar.</p>
     *
     * @return trecho aleatório que será combinado ao prefixo público do token
     */
    private static String randomToken() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Monta a versão mascarada do token para aparecer na tela sem revelar o
     * segredo completo.
     *
     * @param token token completo recém-gerado
     * @return prévia segura, com prefixo e apenas os últimos caracteres visíveis
     */
    private static String preview(String token) {
        String suffix = token.length() <= 4 ? token : token.substring(token.length() - 4);
        return TOKEN_PREFIX + "••••" + suffix;
    }

    /**
     * Converte o token completo em uma impressão digital segura para gravação.
     *
     * <p>Uso interno para permitir validações futuras sem guardar a chave em
     * texto aberto no banco de dados.</p>
     *
     * @param value token completo que precisa ser protegido
     * @return hash em Base64 URL-safe
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
