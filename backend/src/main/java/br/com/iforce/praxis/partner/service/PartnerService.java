package br.com.iforce.praxis.partner.service;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.partner.dto.CreatePartnerClientRequest;
import br.com.iforce.praxis.partner.dto.CreatePartnerSpecialistRequest;
import br.com.iforce.praxis.partner.dto.PartnerModuleResponse;
import br.com.iforce.praxis.partner.dto.UpdatePartnerCatalogRequest;
import br.com.iforce.praxis.partner.persistence.entity.PartnerCatalogAccessEntity;
import br.com.iforce.praxis.partner.persistence.entity.PartnerClientEntity;
import br.com.iforce.praxis.partner.persistence.repository.PartnerCatalogAccessRepository;
import br.com.iforce.praxis.partner.persistence.repository.PartnerClientRepository;
import br.com.iforce.praxis.shared.integration.IntegrationTokenEntity;
import br.com.iforce.praxis.shared.integration.IntegrationTokenRepository;
import br.com.iforce.praxis.shared.integration.model.IntegrationProvider;
import br.com.iforce.praxis.shared.integration.model.IntegrationStatus;
import br.com.iforce.praxis.shared.integration.model.IntegrationType;
import br.com.iforce.praxis.shared.integration.persistence.entity.EmpresaIntegrationEntity;
import br.com.iforce.praxis.shared.integration.persistence.repository.EmpresaIntegrationRepository;
import br.com.iforce.praxis.team.dto.InviteTeamUserRequest;
import br.com.iforce.praxis.team.dto.InviteTeamUserResponse;
import br.com.iforce.praxis.team.dto.TeamUserResponse;
import br.com.iforce.praxis.team.service.TeamService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class PartnerService {

    public static final String PARTNER_SPECIALIST_ROLE = "PARTNER_SPECIALIST";
    private static final String EMPRESA_ROLE = "EMPRESA";
    private static final Set<IntegrationProvider> SUPPORTED_CLIENT_PROVIDERS = Set.of(
            IntegrationProvider.GUPY,
            IntegrationProvider.RECRUTEI
    );
    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();
    private final PartnerClientRepository partnerClientRepository;
    private final PartnerCatalogAccessRepository partnerCatalogAccessRepository;
    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;
    private final IntegrationTokenRepository integrationTokenRepository;
    private final EmpresaIntegrationRepository empresaIntegrationRepository;
    private final SimulationCatalogService simulationCatalogService;
    private final TeamService teamService;

    public PartnerService(
            PartnerClientRepository partnerClientRepository,
            PartnerCatalogAccessRepository partnerCatalogAccessRepository,
            UserRepository userRepository,
            EmpresaRepository empresaRepository,
            IntegrationTokenRepository integrationTokenRepository,
            EmpresaIntegrationRepository empresaIntegrationRepository,
            SimulationCatalogService simulationCatalogService,
            TeamService teamService
    ) {
        this.partnerClientRepository = partnerClientRepository;
        this.partnerCatalogAccessRepository = partnerCatalogAccessRepository;
        this.userRepository = userRepository;
        this.empresaRepository = empresaRepository;
        this.integrationTokenRepository = integrationTokenRepository;
        this.empresaIntegrationRepository = empresaIntegrationRepository;
        this.simulationCatalogService = simulationCatalogService;
        this.teamService = teamService;
    }

    @Transactional(readOnly = true)
    public List<PartnerModuleResponse.Specialist> listSpecialists(String empresaId) {
        return userRepository.findByEmpresaIdAndRole(empresaId, PARTNER_SPECIALIST_ROLE).stream()
                .map(this::toSpecialist)
                .toList();
    }

    @Transactional
    public InviteTeamUserResponse inviteSpecialist(
            String actorUserId,
            String empresaId,
            CreatePartnerSpecialistRequest request
    ) {
        requirePartnerManager(actorUserId, empresaId);
        InviteTeamUserResponse invited = teamService.inviteUser(
                actorUserId,
                empresaId,
                new InviteTeamUserRequest(request.name(), request.email())
        );
        UserEntity user = requireUser(empresaId, invited.user().id());
        user.setRoles(new HashSet<>(Set.of(PARTNER_SPECIALIST_ROLE)));

        TeamUserResponse response = new TeamUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
        return new InviteTeamUserResponse(response, invited.inviteUrl());
    }

    @Transactional
    public PartnerModuleResponse.Specialist promoteSpecialist(
            String actorUserId,
            String empresaId,
            Long userId
    ) {
        requirePartnerManager(actorUserId, empresaId);
        UserEntity user = requireUser(empresaId, userId);
        user.setRoles(new HashSet<>(Set.of(PARTNER_SPECIALIST_ROLE)));
        return toSpecialist(user);
    }

    @Transactional
    public void removeSpecialist(String actorUserId, String empresaId, Long userId) {
        requirePartnerManager(actorUserId, empresaId);
        UserEntity user = requireUser(empresaId, userId);
        user.setRoles(new HashSet<>(Set.of(EMPRESA_ROLE)));
    }

    @Transactional(readOnly = true)
    public List<PartnerModuleResponse.Client> listClients(String empresaId) {
        return partnerClientRepository.findByEmpresaIdOrderByNameAsc(empresaId).stream()
                .map(client -> toClientResponse(
                        client,
                        integrationTokenRepository.existsByEmpresaIdAndPartnerClientIdAndProvider(
                                empresaId,
                                client.getId(),
                                providerKey(client.getProvider())
                        ),
                        partnerCatalogAccessRepository.countByEmpresaIdAndPartnerClientIdAndActiveTrue(
                                empresaId,
                                client.getId()
                        )
                ))
                .toList();
    }

    @Transactional
    public PartnerModuleResponse.Client createClient(
            String actorUserId,
            String empresaId,
            CreatePartnerClientRequest request
    ) {
        requirePartnerManager(actorUserId, empresaId);
        requireSupportedProvider(request.provider());
        String externalCompanyId = request.externalCompanyId().trim();
        if (partnerClientRepository.existsByEmpresaIdAndProviderAndExternalCompanyId(
                empresaId,
                request.provider(),
                externalCompanyId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Já existe um cliente com esse identificador para o provedor informado."
            );
        }

        Instant now = Instant.now();
        PartnerClientEntity client = new PartnerClientEntity();
        client.setId("pcli_" + UUID.randomUUID().toString().replace("-", ""));
        client.setEmpresaId(empresaId);
        client.setName(request.name().trim());
        client.setExternalCompanyId(externalCompanyId);
        client.setProvider(request.provider());
        client.setActive(true);
        client.setCreatedAt(now);
        client.setUpdatedAt(now);
        PartnerClientEntity saved = partnerClientRepository.save(client);
        return toClientResponse(saved, false, 0);
    }

    @Transactional
    public PartnerModuleResponse.Client setClientActive(
            String actorUserId,
            String empresaId,
            String clientId,
            boolean active
    ) {
        requirePartnerManager(actorUserId, empresaId);
        PartnerClientEntity client = requireClient(empresaId, clientId);
        client.setActive(active);
        client.setUpdatedAt(Instant.now());
        if (!active) {
            integrationTokenRepository.deleteByPartnerClientId(clientId);
        }
        return toClientResponse(
                client,
                active && integrationTokenRepository.existsByEmpresaIdAndPartnerClientIdAndProvider(
                        empresaId,
                        clientId,
                        providerKey(client.getProvider())
                ),
                partnerCatalogAccessRepository.countByEmpresaIdAndPartnerClientIdAndActiveTrue(
                        empresaId,
                        clientId
                )
        );
    }

    @Transactional
    public PartnerModuleResponse.Token rotateClientToken(
            String actorUserId,
            String empresaId,
            String clientId
    ) {
        requirePartnerManager(actorUserId, empresaId);
        PartnerClientEntity client = requireClient(empresaId, clientId);
        requireSupportedProvider(client.getProvider());
        if (!client.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ative o cliente antes de gerar o token.");
        }

        EmpresaEntity empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa não encontrada."));
        String provider = providerKey(client.getProvider());
        String token = generateToken();
        integrationTokenRepository.deleteByEmpresaIdAndPartnerClientIdAndProvider(empresaId, clientId, provider);
        integrationTokenRepository.flush();

        IntegrationTokenEntity entity = new IntegrationTokenEntity();
        entity.setEmpresa(empresa);
        entity.setProvider(provider);
        entity.setTokenHash(sha256(token));
        entity.setPartnerClientId(clientId);
        entity.setClientCompanyId(client.getExternalCompanyId());
        entity.setCreatedAt(Instant.now());
        IntegrationTokenEntity saved = integrationTokenRepository.save(entity);
        ensureProviderIntegration(empresa, client.getProvider(), saved.getCreatedAt());

        return new PartnerModuleResponse.Token(
                clientId,
                client.getProvider(),
                token,
                saved.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<PartnerModuleResponse.CatalogItem> listCatalog(String empresaId, String clientId) {
        requireClient(empresaId, clientId);
        Set<String> assigned = assignedSimulationIds(empresaId, clientId);
        return simulationCatalogService.findPublished(empresaId).stream()
                .map(simulation -> new PartnerModuleResponse.CatalogItem(
                        simulation.id(),
                        simulation.name(),
                        simulation.description(),
                        assigned.contains(simulation.id())
                ))
                .toList();
    }

    @Transactional
    public List<PartnerModuleResponse.CatalogItem> updateCatalog(
            String actorUserId,
            String empresaId,
            String clientId,
            UpdatePartnerCatalogRequest request
    ) {
        requirePartnerManager(actorUserId, empresaId);
        PartnerClientEntity client = requireClient(empresaId, clientId);
        if (!client.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ative o cliente antes de liberar testes.");
        }

        Set<String> requestedIds = new HashSet<>(request.simulationIds());
        for (String simulationId : requestedIds) {
            simulationCatalogService.findPublishedById(empresaId, simulationId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "O teste " + simulationId + " não está publicado para este parceiro."
                    ));
        }

        List<PartnerCatalogAccessEntity> current = partnerCatalogAccessRepository
                .findByEmpresaIdAndPartnerClientIdAndActiveTrueOrderByCreatedAtAsc(empresaId, clientId);
        for (PartnerCatalogAccessEntity access : current) {
            if (!requestedIds.contains(access.getSimulationId())) {
                access.setActive(false);
            }
        }

        for (String simulationId : requestedIds) {
            PartnerCatalogAccessEntity access = partnerCatalogAccessRepository
                    .findByEmpresaIdAndPartnerClientIdAndSimulationId(empresaId, clientId, simulationId)
                    .orElseGet(() -> newCatalogAccess(empresaId, clientId, simulationId));
            access.setActive(true);
            partnerCatalogAccessRepository.save(access);
        }

        return listCatalog(empresaId, clientId);
    }

    private void ensureProviderIntegration(
            EmpresaEntity empresa,
            IntegrationProvider provider,
            Instant configuredAt
    ) {
        EmpresaIntegrationEntity integration = empresaIntegrationRepository
                .findFirstByEmpresaIdAndProvider(empresa.getId(), provider)
                .orElseGet(EmpresaIntegrationEntity::new);
        boolean newIntegration = integration.getId() == null;
        if (newIntegration) {
            integration.setEmpresa(empresa);
            integration.setProvider(provider);
            integration.setCreatedAt(configuredAt);
        }
        integration.setType(IntegrationType.ATS);
        if (newIntegration
                || integration.getStatus() == IntegrationStatus.NAO_CONFIGURADA
                || integration.getStatus() == IntegrationStatus.DESATIVADA) {
            integration.setStatus(IntegrationStatus.PENDENTE);
        }
        integration.setConfiguredAt(configuredAt);
        integration.setDisabledAt(null);
        integration.setLastErrorMessage(null);
        integration.setUpdatedAt(configuredAt);
        empresaIntegrationRepository.save(integration);
    }

    private PartnerCatalogAccessEntity newCatalogAccess(
            String empresaId,
            String clientId,
            String simulationId
    ) {
        PartnerCatalogAccessEntity access = new PartnerCatalogAccessEntity();
        access.setEmpresaId(empresaId);
        access.setPartnerClientId(clientId);
        access.setSimulationId(simulationId);
        access.setActive(true);
        access.setCreatedAt(Instant.now());
        return access;
    }

    private Set<String> assignedSimulationIds(String empresaId, String clientId) {
        return partnerCatalogAccessRepository
                .findByEmpresaIdAndPartnerClientIdAndActiveTrueOrderByCreatedAtAsc(empresaId, clientId)
                .stream()
                .map(PartnerCatalogAccessEntity::getSimulationId)
                .collect(java.util.stream.Collectors.toSet());
    }

    private PartnerClientEntity requireClient(String empresaId, String clientId) {
        return partnerClientRepository.findByIdAndEmpresaId(clientId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente do parceiro não encontrado."));
    }

    private UserEntity requireUser(String empresaId, Long userId) {
        return userRepository.findByIdAndEmpresaId(userId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private void requirePartnerManager(String actorUserId, String empresaId) {
        try {
            Long userId = Long.parseLong(actorUserId);
            UserEntity actor = requireUser(empresaId, userId);
            if (actor.getRoles().contains(PARTNER_SPECIALIST_ROLE)) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Especialistas podem criar testes, mas não administrar clientes ou outros especialistas."
                );
            }
        } catch (NumberFormatException ignored) {
            // Usuário técnico usado apenas quando a segurança está desabilitada.
        }
    }

    private void requireSupportedProvider(IntegrationProvider provider) {
        if (!SUPPORTED_CLIENT_PROVIDERS.contains(provider)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "O catálogo por cliente está disponível para Gupy e Recrutei."
            );
        }
    }

    private PartnerModuleResponse.Specialist toSpecialist(UserEntity user) {
        return new PartnerModuleResponse.Specialist(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }

    private PartnerModuleResponse.Client toClientResponse(
            PartnerClientEntity client,
            boolean tokenConfigured,
            long assignedTests
    ) {
        return new PartnerModuleResponse.Client(
                client.getId(),
                client.getName(),
                client.getExternalCompanyId(),
                client.getProvider(),
                client.isActive(),
                tokenConfigured,
                assignedTests,
                client.getCreatedAt()
        );
    }

    private String providerKey(IntegrationProvider provider) {
        return provider.name().toLowerCase(Locale.ROOT);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return "prx_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }
}
