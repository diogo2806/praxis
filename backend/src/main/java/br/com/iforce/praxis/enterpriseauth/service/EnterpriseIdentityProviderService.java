package br.com.iforce.praxis.enterpriseauth.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.auth.service.CurrentUserService;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.AuditEventResponse;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.CallbackResponse;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.DiscoveryResponse;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.IdentityProviderResponse;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.Protocol;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.ProviderTestResponse;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.SaveIdentityProviderRequest;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.StartLoginRequest;
import br.com.iforce.praxis.enterpriseauth.dto.EnterpriseAuthDtos.StartLoginResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EnterpriseIdentityProviderService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int LOGIN_REQUEST_TTL_MINUTES = 10;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final CurrentEmpresaService currentEmpresaService;
    private final CurrentUserService currentUserService;
    private final EnterpriseTokenService tokenService;
    private final HttpClient httpClient;

    public EnterpriseIdentityProviderService(
            NamedParameterJdbcTemplate jdbcTemplate,
            ObjectMapper objectMapper,
            CurrentEmpresaService currentEmpresaService,
            CurrentUserService currentUserService,
            EnterpriseTokenService tokenService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.currentEmpresaService = currentEmpresaService;
        this.currentUserService = currentUserService;
        this.tokenService = tokenService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Transactional
    public IdentityProviderResponse create(SaveIdentityProviderRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actor = currentUserService.requiredUserId();
        UUID id = UUID.randomUUID();
        validateConfiguration(request);
        jdbcTemplate.update("""
                INSERT INTO enterprise_identity_providers (
                    id, empresa_id, display_name, protocol, issuer_uri, client_id,
                    client_secret_env_var, redirect_uri, frontend_success_uri, scopes,
                    allowed_email_domains, default_role, jit_provisioning_enabled,
                    enforce_sso, require_mfa, accepted_mfa_amr_values, active,
                    created_by, created_at, updated_by, updated_at
                ) VALUES (
                    :id, :empresaId, :displayName, :protocol, :issuerUri, :clientId,
                    :secretEnv, :redirectUri, :frontendSuccessUri, :scopes,
                    :domains, :defaultRole, :jit, :enforceSso, :requireMfa,
                    :acceptedAmr, :active, :actor, :now, :actor, :now
                )
                """, providerParameters(request, empresaId, actor)
                .addValue("id", id)
                .addValue("now", Instant.now()));
        audit(empresaId, id, "PROVIDER_CREATED", "SUCCESS", actor, null, null,
                "Provedor corporativo criado.");
        return get(id.toString());
    }

    @Transactional
    public IdentityProviderResponse update(String providerId, SaveIdentityProviderRequest request) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        String actor = currentUserService.requiredUserId();
        validateConfiguration(request);
        int updated = jdbcTemplate.update("""
                UPDATE enterprise_identity_providers
                   SET display_name = :displayName,
                       protocol = :protocol,
                       issuer_uri = :issuerUri,
                       client_id = :clientId,
                       client_secret_env_var = :secretEnv,
                       redirect_uri = :redirectUri,
                       frontend_success_uri = :frontendSuccessUri,
                       scopes = :scopes,
                       allowed_email_domains = :domains,
                       default_role = :defaultRole,
                       jit_provisioning_enabled = :jit,
                       enforce_sso = :enforceSso,
                       require_mfa = :requireMfa,
                       accepted_mfa_amr_values = :acceptedAmr,
                       active = :active,
                       updated_by = :actor,
                       updated_at = :now
                 WHERE id = CAST(:providerId AS UUID)
                   AND empresa_id = :empresaId
                """, providerParameters(request, empresaId, actor)
                .addValue("providerId", providerId)
                .addValue("now", Instant.now()));
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Provedor corporativo não encontrado.");
        }
        audit(empresaId, UUID.fromString(providerId), "PROVIDER_UPDATED", "SUCCESS", actor,
                null, null, "Configuração corporativa atualizada.");
        return get(providerId);
    }

    @Transactional(readOnly = true)
    public List<IdentityProviderResponse> list() {
        return jdbcTemplate.query("""
                SELECT * FROM enterprise_identity_providers
                 WHERE empresa_id = :empresaId
                 ORDER BY display_name
                """, new MapSqlParameterSource("empresaId", currentEmpresaService.requiredEmpresaId()),
                (resultSet, rowNumber) -> mapProviderResponse(resultSet));
    }

    @Transactional(readOnly = true)
    public IdentityProviderResponse get(String providerId) {
        List<IdentityProviderResponse> rows = jdbcTemplate.query("""
                SELECT * FROM enterprise_identity_providers
                 WHERE id = CAST(:providerId AS UUID)
                   AND empresa_id = :empresaId
                """, new MapSqlParameterSource()
                .addValue("providerId", providerId)
                .addValue("empresaId", currentEmpresaService.requiredEmpresaId()),
                (resultSet, rowNumber) -> mapProviderResponse(resultSet));
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Provedor corporativo não encontrado.");
        }
        return rows.getFirst();
    }

    @Transactional
    public ProviderTestResponse test(String providerId) {
        ProviderRecord provider = requireProviderForCompany(providerId);
        Instant testedAt = Instant.now();
        try {
            OidcDocument document = discover(provider.issuerUri());
            requireClientSecret(provider);
            ProviderTestResponse response = new ProviderTestResponse(
                    true,
                    document.issuer(),
                    document.authorizationEndpoint(),
                    document.tokenEndpoint(),
                    document.jwksUri(),
                    "Discovery OIDC e variável de segredo validadas.",
                    testedAt
            );
            updateTestStatus(provider.id(), "SUCCESS", response.message(), testedAt);
            audit(provider.empresaId(), provider.id(), "PROVIDER_TESTED", "SUCCESS",
                    currentUserService.requiredUserId(), null, null, response.message());
            return response;
        } catch (RuntimeException exception) {
            String message = safeMessage(exception);
            updateTestStatus(provider.id(), "ERROR", message, testedAt);
            audit(provider.empresaId(), provider.id(), "PROVIDER_TESTED", "ERROR",
                    currentUserService.requiredUserId(), null, null, message);
            return new ProviderTestResponse(false, provider.issuerUri(), null, null, null, message, testedAt);
        }
    }

    @Transactional(readOnly = true)
    public DiscoveryResponse discoverByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        String domain = emailDomain(normalizedEmail);
        List<ProviderRecord> providers = jdbcTemplate.query("""
                SELECT * FROM enterprise_identity_providers
                 WHERE active = TRUE
                 ORDER BY enforce_sso DESC, updated_at DESC
                """, new MapSqlParameterSource(), (resultSet, rowNumber) -> mapProvider(resultSet));
        ProviderRecord provider = providers.stream()
                .filter(item -> item.allowedDomains().contains(domain))
                .findFirst()
                .orElse(null);
        if (provider == null) {
            return new DiscoveryResponse(false, false, true, null, null, null,
                    null, false, "Nenhum provedor corporativo ativo para este domínio.");
        }
        return new DiscoveryResponse(
                true,
                provider.enforceSso(),
                !provider.enforceSso(),
                provider.id().toString(),
                provider.displayName(),
                Protocol.OIDC,
                "/api/v1/enterprise-auth/providers/" + provider.id() + "/start",
                provider.requireMfa(),
                provider.enforceSso()
                        ? "Use o acesso corporativo para continuar."
                        : "O acesso corporativo está disponível para este domínio."
        );
    }

    @Transactional
    public StartLoginResponse start(
            String providerId,
            StartLoginRequest request,
            String ipAddress,
            String userAgent
    ) {
        ProviderRecord provider = requireActiveProvider(providerId);
        if (request.email() != null && !request.email().isBlank()) {
            String email = normalizeEmail(request.email());
            if (!provider.allowedDomains().contains(emailDomain(email))) {
                audit(provider.empresaId(), provider.id(), "LOGIN_STARTED", "DENIED", email,
                        ipAddress, userAgent, "Domínio de e-mail não autorizado.");
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "O domínio do e-mail não está autorizado para este provedor.");
            }
        }
        OidcDocument document = discover(provider.issuerUri());
        String state = randomUrlToken(32);
        String nonce = randomUrlToken(32);
        String verifier = randomUrlToken(64);
        String challenge = base64Url(sha256Bytes(verifier));
        Instant expiresAt = Instant.now().plus(LOGIN_REQUEST_TTL_MINUTES, ChronoUnit.MINUTES);
        jdbcTemplate.update("""
                INSERT INTO enterprise_sso_login_requests (
                    id, provider_id, empresa_id, state_hash, nonce_hash, pkce_verifier,
                    return_uri, requested_email, created_at, expires_at
                ) VALUES (
                    :id, :providerId, :empresaId, :stateHash, :nonceHash, :verifier,
                    :returnUri, :requestedEmail, :createdAt, :expiresAt
                )
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("providerId", provider.id())
                .addValue("empresaId", provider.empresaId())
                .addValue("stateHash", sha256(state))
                .addValue("nonceHash", sha256(nonce))
                .addValue("verifier", verifier)
                .addValue("returnUri", request.returnUri().trim())
                .addValue("requestedEmail", trimToNull(request.email()))
                .addValue("createdAt", Instant.now())
                .addValue("expiresAt", expiresAt));
        String authorizationUrl = document.authorizationEndpoint()
                + "?response_type=code"
                + "&client_id=" + encode(provider.clientId())
                + "&redirect_uri=" + encode(provider.redirectUri())
                + "&scope=" + encode(provider.scopes())
                + "&state=" + encode(state)
                + "&nonce=" + encode(nonce)
                + "&code_challenge=" + encode(challenge)
                + "&code_challenge_method=S256";
        if (request.email() != null && !request.email().isBlank()) {
            authorizationUrl += "&login_hint=" + encode(normalizeEmail(request.email()));
        }
        audit(provider.empresaId(), provider.id(), "LOGIN_STARTED", "SUCCESS",
                trimToNull(request.email()), ipAddress, userAgent, "Fluxo OIDC iniciado com PKCE.");
        return new StartLoginResponse(authorizationUrl, expiresAt);
    }

    @Transactional
    public CallbackResponse callback(
            String state,
            String code,
            String ipAddress,
            String userAgent
    ) {
        LoginRequestRecord loginRequest = requireLoginRequest(state);
        ProviderRecord provider = requireActiveProvider(loginRequest.providerId().toString());
        OidcDocument document = discover(provider.issuerUri());
        try {
            JsonNode tokenResponse = exchangeCode(provider, document, code, loginRequest.pkceVerifier());
            String idToken = tokenResponse.path("id_token").asText(null);
            if (idToken == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "O provedor não retornou id_token.");
            }
            JsonNode claims = verifyIdToken(provider, document, idToken, loginRequest.nonceHash());
            String subject = requiredClaim(claims, "sub");
            String email = normalizeEmail(requiredClaim(claims, "email"));
            if (claims.has("email_verified") && !claims.path("email_verified").asBoolean(false)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "O provedor não confirmou o e-mail corporativo.");
            }
            if (!provider.allowedDomains().contains(emailDomain(email))) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "O domínio do e-mail retornado não está autorizado.");
            }
            boolean mfaVerified = verifyMfa(provider, claims);
            String displayName = claims.path("name").asText(email);
            IdentityRecord identity = findIdentity(provider.id(), subject);
            if (identity == null && !provider.jitProvisioningEnabled()) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "A identidade não está provisionada e o provisionamento automático está desativado.");
            }
            upsertIdentity(provider, subject, email, displayName, mfaVerified);
            EnterpriseTokenService.IssuedToken issued = tokenService.issue(
                    subject,
                    email,
                    displayName,
                    provider.empresaId(),
                    List.of(provider.defaultRole()),
                    mfaVerified,
                    provider.id().toString()
            );
            consumeLoginRequest(loginRequest.id());
            audit(provider.empresaId(), provider.id(), "LOGIN_COMPLETED", "SUCCESS", email,
                    ipAddress, userAgent, mfaVerified ? "MFA confirmada pelo IdP." : "MFA não exigida.");
            return new CallbackResponse(
                    issued.value(),
                    "Bearer",
                    issued.expiresInSeconds(),
                    email,
                    displayName,
                    provider.empresaId(),
                    List.of(provider.defaultRole()),
                    mfaVerified,
                    loginRequest.returnUri()
            );
        } catch (RuntimeException exception) {
            consumeLoginRequest(loginRequest.id());
            audit(provider.empresaId(), provider.id(), "LOGIN_COMPLETED", "DENIED",
                    loginRequest.requestedEmail(), ipAddress, userAgent, safeMessage(exception));
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> auditEvents(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return jdbcTemplate.query("""
                SELECT id, event_type, outcome, actor_identifier, ip_address,
                       user_agent, detail, occurred_at
                  FROM enterprise_auth_audit_events
                 WHERE empresa_id = :empresaId
                 ORDER BY occurred_at DESC
                 LIMIT :limit
                """, new MapSqlParameterSource()
                .addValue("empresaId", currentEmpresaService.requiredEmpresaId())
                .addValue("limit", safeLimit),
                (resultSet, rowNumber) -> new AuditEventResponse(
                        resultSet.getString("id"),
                        resultSet.getString("event_type"),
                        resultSet.getString("outcome"),
                        resultSet.getString("actor_identifier"),
                        resultSet.getString("ip_address"),
                        resultSet.getString("user_agent"),
                        resultSet.getString("detail"),
                        resultSet.getTimestamp("occurred_at").toInstant()
                ));
    }

    @Transactional(readOnly = true)
    public boolean isPasswordLoginBlocked(String email) {
        String normalized = normalizeEmail(email);
        String domain = emailDomain(normalized);
        List<String> domains = jdbcTemplate.queryForList("""
                SELECT allowed_email_domains
                  FROM enterprise_identity_providers
                 WHERE active = TRUE AND enforce_sso = TRUE
                """, new MapSqlParameterSource(), String.class);
        return domains.stream()
                .flatMap(value -> splitCsv(value).stream())
                .anyMatch(domain::equals);
    }

    private JsonNode exchangeCode(
            ProviderRecord provider,
            OidcDocument document,
            String code,
            String verifier
    ) {
        String secret = requireClientSecret(provider);
        String form = "grant_type=authorization_code"
                + "&code=" + encode(code)
                + "&redirect_uri=" + encode(provider.redirectUri())
                + "&client_id=" + encode(provider.clientId())
                + "&client_secret=" + encode(secret)
                + "&code_verifier=" + encode(verifier);
        HttpRequest request = HttpRequest.newBuilder(URI.create(document.tokenEndpoint()))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        JsonNode response = sendJson(request);
        if (response.has("error")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "O provedor recusou o código: " + response.path("error").asText());
        }
        return response;
    }

    private JsonNode verifyIdToken(
            ProviderRecord provider,
            OidcDocument document,
            String idToken,
            String expectedNonceHash
    ) {
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "id_token inválido.");
        }
        try {
            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            JsonNode claims = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!"RS256".equals(header.path("alg").asText())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Somente assinaturas OIDC RS256 são aceitas.");
            }
            String kid = requiredClaim(header, "kid");
            RSAPublicKey publicKey = resolveSigningKey(document.jwksUri(), kid);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
            if (!verifier.verify(Base64.getUrlDecoder().decode(parts[2]))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Assinatura do id_token inválida.");
            }
            if (!provider.issuerUri().equals(claims.path("iss").asText())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Issuer OIDC divergente.");
            }
            if (!audienceContains(claims.path("aud"), provider.clientId())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Audience OIDC divergente.");
            }
            if (claims.path("exp").asLong(0) <= Instant.now().getEpochSecond()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "id_token expirado.");
            }
            String nonce = requiredClaim(claims, "nonce");
            if (!MessageDigest.isEqual(
                    expectedNonceHash.getBytes(StandardCharsets.UTF_8),
                    sha256(nonce).getBytes(StandardCharsets.UTF_8))) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nonce OIDC inválido.");
            }
            return claims;
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Não foi possível validar o id_token.");
        }
    }

    private RSAPublicKey resolveSigningKey(String jwksUri, String kid) throws Exception {
        JsonNode jwks = sendJson(HttpRequest.newBuilder(URI.create(jwksUri))
                .timeout(java.time.Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build());
        for (JsonNode key : jwks.path("keys")) {
            if (kid.equals(key.path("kid").asText()) && "RSA".equals(key.path("kty").asText())) {
                BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(key.path("n").asText()));
                BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(key.path("e").asText()));
                return (RSAPublicKey) KeyFactory.getInstance("RSA")
                        .generatePublic(new RSAPublicKeySpec(modulus, exponent));
            }
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "Chave pública do id_token não encontrada.");
    }

    private boolean verifyMfa(ProviderRecord provider, JsonNode claims) {
        if (!provider.requireMfa()) {
            return false;
        }
        Set<String> accepted = new LinkedHashSet<>(provider.acceptedMfaAmrValues());
        boolean verified = false;
        JsonNode amr = claims.path("amr");
        if (amr.isArray()) {
            for (JsonNode value : amr) {
                if (accepted.contains(value.asText().toLowerCase(Locale.ROOT))) {
                    verified = true;
                    break;
                }
            }
        }
        if (!verified) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "O provedor não comprovou autenticação multifator. Valores AMR aceitos: "
                            + String.join(", ", accepted) + ".");
        }
        return true;
    }

    private OidcDocument discover(String issuerUri) {
        String normalized = issuerUri.endsWith("/")
                ? issuerUri.substring(0, issuerUri.length() - 1)
                : issuerUri;
        JsonNode document = sendJson(HttpRequest.newBuilder(
                        URI.create(normalized + "/.well-known/openid-configuration"))
                .timeout(java.time.Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build());
        return new OidcDocument(
                requiredClaim(document, "issuer"),
                requiredClaim(document, "authorization_endpoint"),
                requiredClaim(document, "token_endpoint"),
                requiredClaim(document, "jwks_uri")
        );
    }

    private JsonNode sendJson(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "O provedor respondeu HTTP " + response.statusCode() + ".");
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "A comunicação com o provedor foi interrompida.");
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Não foi possível comunicar com o provedor OIDC.");
        }
    }

    private LoginRequestRecord requireLoginRequest(String state) {
        List<LoginRequestRecord> rows = jdbcTemplate.query("""
                SELECT id, provider_id, empresa_id, nonce_hash, pkce_verifier,
                       return_uri, requested_email, expires_at
                  FROM enterprise_sso_login_requests
                 WHERE state_hash = :stateHash
                   AND consumed_at IS NULL
                   AND expires_at > :now
                """, new MapSqlParameterSource()
                .addValue("stateHash", sha256(state))
                .addValue("now", Instant.now()),
                (resultSet, rowNumber) -> new LoginRequestRecord(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("provider_id", UUID.class),
                        resultSet.getString("empresa_id"),
                        resultSet.getString("nonce_hash"),
                        resultSet.getString("pkce_verifier"),
                        resultSet.getString("return_uri"),
                        resultSet.getString("requested_email"),
                        resultSet.getTimestamp("expires_at").toInstant()
                ));
        if (rows.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Estado OIDC inválido, expirado ou já utilizado.");
        }
        return rows.getFirst();
    }

    private ProviderRecord requireProviderForCompany(String providerId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        ProviderRecord provider = findProvider(providerId);
        if (provider == null || !empresaId.equals(provider.empresaId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Provedor corporativo não encontrado.");
        }
        return provider;
    }

    private ProviderRecord requireActiveProvider(String providerId) {
        ProviderRecord provider = findProvider(providerId);
        if (provider == null || !provider.active()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Provedor corporativo ativo não encontrado.");
        }
        return provider;
    }

    private ProviderRecord findProvider(String providerId) {
        List<ProviderRecord> rows = jdbcTemplate.query("""
                SELECT * FROM enterprise_identity_providers
                 WHERE id = CAST(:providerId AS UUID)
                """, new MapSqlParameterSource("providerId", providerId),
                (resultSet, rowNumber) -> mapProvider(resultSet));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private IdentityRecord findIdentity(UUID providerId, String subject) {
        List<IdentityRecord> rows = jdbcTemplate.query("""
                SELECT id, email, assigned_role
                  FROM enterprise_sso_identities
                 WHERE provider_id = :providerId
                   AND subject_identifier = :subject
                   AND disabled_at IS NULL
                """, new MapSqlParameterSource()
                .addValue("providerId", providerId)
                .addValue("subject", subject),
                (resultSet, rowNumber) -> new IdentityRecord(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("email"),
                        resultSet.getString("assigned_role")
                ));
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private void upsertIdentity(
            ProviderRecord provider,
            String subject,
            String email,
            String displayName,
            boolean mfaVerified
    ) {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO enterprise_sso_identities (
                    id, provider_id, empresa_id, subject_identifier, email, display_name,
                    assigned_role, last_mfa_verified_at, first_login_at, last_login_at
                ) VALUES (
                    :id, :providerId, :empresaId, :subject, :email, :displayName,
                    :role, :mfaAt, :now, :now
                )
                ON CONFLICT (provider_id, subject_identifier) DO UPDATE SET
                    email = EXCLUDED.email,
                    display_name = EXCLUDED.display_name,
                    last_mfa_verified_at = EXCLUDED.last_mfa_verified_at,
                    last_login_at = EXCLUDED.last_login_at
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("providerId", provider.id())
                .addValue("empresaId", provider.empresaId())
                .addValue("subject", subject)
                .addValue("email", email)
                .addValue("displayName", displayName)
                .addValue("role", provider.defaultRole())
                .addValue("mfaAt", mfaVerified ? now : null)
                .addValue("now", now));
    }

    private void consumeLoginRequest(UUID id) {
        jdbcTemplate.update("""
                UPDATE enterprise_sso_login_requests
                   SET consumed_at = COALESCE(consumed_at, :now)
                 WHERE id = :id
                """, new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("now", Instant.now()));
    }

    private void updateTestStatus(UUID providerId, String status, String message, Instant testedAt) {
        jdbcTemplate.update("""
                UPDATE enterprise_identity_providers
                   SET last_test_status = :status,
                       last_test_message = :message,
                       last_tested_at = :testedAt
                 WHERE id = :providerId
                """, new MapSqlParameterSource()
                .addValue("status", status)
                .addValue("message", message)
                .addValue("testedAt", testedAt)
                .addValue("providerId", providerId));
    }

    private void audit(
            String empresaId,
            UUID providerId,
            String eventType,
            String outcome,
            String actor,
            String ipAddress,
            String userAgent,
            String detail
    ) {
        jdbcTemplate.update("""
                INSERT INTO enterprise_auth_audit_events (
                    id, empresa_id, provider_id, event_type, outcome, actor_identifier,
                    ip_address, user_agent, detail, occurred_at
                ) VALUES (
                    :id, :empresaId, :providerId, :eventType, :outcome, :actor,
                    :ipAddress, :userAgent, :detail, :occurredAt
                )
                """, new MapSqlParameterSource()
                .addValue("id", UUID.randomUUID())
                .addValue("empresaId", empresaId)
                .addValue("providerId", providerId)
                .addValue("eventType", eventType)
                .addValue("outcome", outcome)
                .addValue("actor", actor)
                .addValue("ipAddress", ipAddress)
                .addValue("userAgent", userAgent)
                .addValue("detail", detail)
                .addValue("occurredAt", Instant.now()));
    }

    private MapSqlParameterSource providerParameters(
            SaveIdentityProviderRequest request,
            String empresaId,
            String actor
    ) {
        return new MapSqlParameterSource()
                .addValue("empresaId", empresaId)
                .addValue("displayName", request.displayName().trim())
                .addValue("protocol", request.protocol().name())
                .addValue("issuerUri", normalizeIssuer(request.issuerUri()))
                .addValue("clientId", request.clientId().trim())
                .addValue("secretEnv", request.clientSecretEnvVar().trim())
                .addValue("redirectUri", request.redirectUri().trim())
                .addValue("frontendSuccessUri", request.frontendSuccessUri().trim())
                .addValue("scopes", normalizeScopes(request.scopes()))
                .addValue("domains", String.join(",", normalizeDomains(request.allowedEmailDomains())))
                .addValue("defaultRole", normalizeRole(request.defaultRole()))
                .addValue("jit", request.jitProvisioningEnabled())
                .addValue("enforceSso", request.enforceSso())
                .addValue("requireMfa", request.requireMfa())
                .addValue("acceptedAmr", String.join(",", normalizeAmr(request.acceptedMfaAmrValues())))
                .addValue("active", request.active())
                .addValue("actor", actor);
    }

    private ProviderRecord mapProvider(ResultSet resultSet) throws SQLException {
        return new ProviderRecord(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("empresa_id"),
                resultSet.getString("display_name"),
                Protocol.valueOf(resultSet.getString("protocol")),
                resultSet.getString("issuer_uri"),
                resultSet.getString("client_id"),
                resultSet.getString("client_secret_env_var"),
                resultSet.getString("redirect_uri"),
                resultSet.getString("frontend_success_uri"),
                resultSet.getString("scopes"),
                splitCsv(resultSet.getString("allowed_email_domains")),
                resultSet.getString("default_role"),
                resultSet.getBoolean("jit_provisioning_enabled"),
                resultSet.getBoolean("enforce_sso"),
                resultSet.getBoolean("require_mfa"),
                splitCsv(resultSet.getString("accepted_mfa_amr_values")),
                resultSet.getBoolean("active")
        );
    }

    private IdentityProviderResponse mapProviderResponse(ResultSet resultSet) throws SQLException {
        return new IdentityProviderResponse(
                resultSet.getString("id"),
                resultSet.getString("display_name"),
                Protocol.valueOf(resultSet.getString("protocol")),
                resultSet.getString("issuer_uri"),
                resultSet.getString("client_id"),
                resultSet.getString("client_secret_env_var"),
                resultSet.getString("redirect_uri"),
                resultSet.getString("frontend_success_uri"),
                splitCsv(resultSet.getString("allowed_email_domains")),
                resultSet.getString("scopes"),
                resultSet.getString("default_role"),
                resultSet.getBoolean("jit_provisioning_enabled"),
                resultSet.getBoolean("enforce_sso"),
                resultSet.getBoolean("require_mfa"),
                splitCsv(resultSet.getString("accepted_mfa_amr_values")),
                resultSet.getBoolean("active"),
                resultSet.getString("last_test_status"),
                resultSet.getString("last_test_message"),
                toInstant(resultSet.getTimestamp("last_tested_at")),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private void validateConfiguration(SaveIdentityProviderRequest request) {
        normalizeIssuer(request.issuerUri());
        normalizeDomains(request.allowedEmailDomains());
        normalizeRole(request.defaultRole());
        if (request.requireMfa() && normalizeAmr(request.acceptedMfaAmrValues()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe pelo menos um valor AMR aceito quando MFA for obrigatória.");
        }
        validateHttpsUrl(request.redirectUri(), "redirectUri");
        validateHttpsUrl(request.frontendSuccessUri(), "frontendSuccessUri");
    }

    private String requireClientSecret(ProviderRecord provider) {
        String secret = System.getenv(provider.clientSecretEnvVar());
        if (secret == null || secret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A variável de ambiente " + provider.clientSecretEnvVar() + " não está configurada.");
        }
        return secret;
    }

    private String normalizeIssuer(String value) {
        validateHttpsUrl(value, "issuerUri");
        String normalized = value.trim();
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }

    private void validateHttpsUrl(String value, String field) {
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    field + " deve ser uma URL HTTPS válida.");
        }
    }

    private List<String> normalizeDomains(List<String> domains) {
        List<String> normalized = domains.stream()
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .map(value -> value.startsWith("@") ? value.substring(1) : value)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe pelo menos um domínio corporativo.");
        }
        return normalized;
    }

    private List<String> normalizeAmr(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private String normalizeScopes(String scopes) {
        LinkedHashSet<String> values = new LinkedHashSet<>(List.of(scopes.trim().split("\\s+")));
        values.add("openid");
        values.add("email");
        return String.join(" ", values);
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_]", "_");
        Set<String> allowed = Set.of(
                "TEAM_MANAGER",
                "PARTNER_MANAGER",
                "ASSESSMENT_EDITOR",
                "RESULTS_ANALYST",
                "OPERATIONS_MANAGER"
        );
        if (!allowed.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Perfil padrão não permitido para provisionamento SSO.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (!normalized.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "E-mail inválido.");
        }
        return normalized;
    }

    private String emailDomain(String email) {
        return email.substring(email.lastIndexOf('@') + 1);
    }

    private boolean audienceContains(JsonNode audience, String clientId) {
        if (audience.isTextual()) {
            return clientId.equals(audience.asText());
        }
        if (audience.isArray()) {
            for (JsonNode value : audience) {
                if (clientId.equals(value.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String requiredClaim(JsonNode node, String field) {
        String value = node.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Claim obrigatória ausente: " + field + ".");
        }
        return value;
    }

    private String randomUrlToken(int bytes) {
        byte[] value = new byte[bytes];
        SECURE_RANDOM.nextBytes(value);
        return base64Url(value);
    }

    private String sha256(String value) {
        return java.util.HexFormat.of().formatHex(sha256Bytes(value));
    }

    private byte[] sha256Bytes(String value) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .collect(Collectors.toList());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private String safeMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private record ProviderRecord(
            UUID id,
            String empresaId,
            String displayName,
            Protocol protocol,
            String issuerUri,
            String clientId,
            String clientSecretEnvVar,
            String redirectUri,
            String frontendSuccessUri,
            String scopes,
            List<String> allowedDomains,
            String defaultRole,
            boolean jitProvisioningEnabled,
            boolean enforceSso,
            boolean requireMfa,
            List<String> acceptedMfaAmrValues,
            boolean active
    ) {
    }

    private record LoginRequestRecord(
            UUID id,
            UUID providerId,
            String empresaId,
            String nonceHash,
            String pkceVerifier,
            String returnUri,
            String requestedEmail,
            Instant expiresAt
    ) {
    }

    private record IdentityRecord(UUID id, String email, String role) {
    }

    private record OidcDocument(
            String issuer,
            String authorizationEndpoint,
            String tokenEndpoint,
            String jwksUri
    ) {
    }
}
