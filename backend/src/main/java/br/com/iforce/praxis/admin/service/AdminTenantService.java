package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.AdminUserResponse;
import br.com.iforce.praxis.admin.dto.CreateTenantAdminRequest;
import br.com.iforce.praxis.admin.dto.CreateTenantAdminResponse;
import br.com.iforce.praxis.admin.dto.InviteUserAdminRequest;
import br.com.iforce.praxis.admin.dto.InviteUserAdminResponse;
import br.com.iforce.praxis.admin.dto.ReactivateTenantAdminRequest;
import br.com.iforce.praxis.admin.dto.TenantAdminDetailResponse;
import br.com.iforce.praxis.admin.dto.TenantAdminSummaryResponse;
import br.com.iforce.praxis.admin.dto.UpdateTenantAdminRequest;
import br.com.iforce.praxis.admin.model.CommercialPlanType;
import br.com.iforce.praxis.admin.model.TenantStatus;
import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.entity.TenantEntity;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.TenantRepository;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orquestra o ciclo de vida dos clientes (tenants) a partir do painel ADMIN.
 *
 * <p>O ADMIN é um operador da plataforma e não pertence ao fluxo de tenant cliente: por isso
 * todas as operações recebem o tenant alvo explicitamente, sem depender do
 * {@code CurrentTenantService}. Cada ação sensível exige motivo e gera evento de auditoria
 * append-only, registrando o operador (ator) e o tenant alvo.</p>
 */
@Service
public class AdminTenantService {

    /** Papel do usuário responsável do cliente. Nunca ADMIN. */
    public static final String EMPRESA_ROLE = "EMPRESA";

    private static final String PLATFORM_TENANT_ID = "PLATFORM";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;
    private final AdminUsageService adminUsageService;
    private final String publicBaseUrl;
    private final int inviteTtlHours;
    private final int usagePeriodDays;

    public AdminTenantService(
            TenantRepository tenantRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata,
            AdminUsageService adminUsageService,
            @Value("${praxis.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${praxis.admin.invite-ttl-hours:168}") int inviteTtlHours,
            @Value("${praxis.admin.usage-period-days:30}") int usagePeriodDays
    ) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
        this.adminUsageService = adminUsageService;
        this.publicBaseUrl = publicBaseUrl;
        this.inviteTtlHours = inviteTtlHours;
        this.usagePeriodDays = usagePeriodDays;
    }

    // ------------------------------------------------------------------
    // Listagem e detalhe
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<TenantAdminSummaryResponse> list(
            String search,
            TenantStatus status,
            CommercialPlanType plan,
            Instant periodStart,
            Instant periodEnd
    ) {
        Period period = resolvePeriod(periodStart, periodEnd);
        String normalizedSearch = (search == null || search.isBlank())
                ? null
                : "%" + search.toLowerCase().trim() + "%";

        return tenantRepository.search(normalizedSearch, status, plan).stream()
                .map(tenant -> toSummary(tenant, period))
                .toList();
    }

    @Transactional(readOnly = true)
    public TenantAdminDetailResponse detail(String tenantId) {
        TenantEntity tenant = requireClient(tenantId);
        return toDetail(tenant, resolvePeriod(null, null));
    }

    // ------------------------------------------------------------------
    // Cadastro
    // ------------------------------------------------------------------

    @Transactional
    public CreateTenantAdminResponse create(String actorUserId, CreateTenantAdminRequest request) {
        Instant now = Instant.now();

        TenantEntity tenant = new TenantEntity();
        tenant.setId(generateTenantId());
        tenant.setName(request.name().trim());
        tenant.setTradeName(blankToNull(request.tradeName()));
        tenant.setLegalName(blankToNull(request.legalName()));
        tenant.setTaxId(blankToNull(request.taxId()));
        tenant.setCorporateEmail(blankToNull(request.corporateEmail()));
        tenant.setPhone(blankToNull(request.phone()));
        tenant.setWebsite(blankToNull(request.website()));
        tenant.setHealthVertical(request.healthVertical());
        tenant.setCompanyId(resolveCompanyId(request.companyId()));
        tenant.setCommercialPlanType(request.commercialPlanType());
        tenant.setCommercialCondition(blankToNull(request.commercialCondition()));
        tenant.setStatus(request.initialStatus() == null ? TenantStatus.EM_TESTE : request.initialStatus());
        tenant.setCreatedAt(now);
        tenant.setUpdatedAt(now);
        tenantRepository.save(tenant);

        auditEventService.auditAdminAction(
                actorUserId,
                tenant.getId(),
                AuditEventType.ADMIN_TENANT_CREATED,
                "Cliente criado: " + tenant.getName(),
                auditMetadata.of(
                        "name", tenant.getName(),
                        "commercialPlanType", tenant.getCommercialPlanType().name(),
                        "status", tenant.getStatus().name(),
                        "healthVertical", tenant.isHealthVertical(),
                        "companyId", tenant.getCompanyId()
                )
        );

        InvitedUser invited = provisionResponsibleUser(
                actorUserId, tenant.getId(), request.responsibleName(), request.responsibleEmail(),
                request.sendInvite(), now);

        return new CreateTenantAdminResponse(
                toDetail(tenant, resolvePeriod(null, null)),
                invited.user().getId(),
                invited.inviteUrl()
        );
    }

    // ------------------------------------------------------------------
    // Edição de dados / plano / condição comercial
    // ------------------------------------------------------------------

    @Transactional
    public TenantAdminDetailResponse update(
            String actorUserId,
            String tenantId,
            UpdateTenantAdminRequest request
    ) {
        TenantEntity tenant = requireClient(tenantId);

        if (request.name() != null && !request.name().isBlank()) {
            tenant.setName(request.name().trim());
        }
        if (request.tradeName() != null) {
            tenant.setTradeName(blankToNull(request.tradeName()));
        }
        if (request.legalName() != null) {
            tenant.setLegalName(blankToNull(request.legalName()));
        }
        if (request.taxId() != null) {
            tenant.setTaxId(blankToNull(request.taxId()));
        }
        if (request.corporateEmail() != null) {
            tenant.setCorporateEmail(blankToNull(request.corporateEmail()));
        }
        if (request.phone() != null) {
            tenant.setPhone(blankToNull(request.phone()));
        }
        if (request.website() != null) {
            tenant.setWebsite(blankToNull(request.website()));
        }
        if (request.healthVertical() != null) {
            tenant.setHealthVertical(request.healthVertical());
        }

        CommercialPlanType previousPlan = tenant.getCommercialPlanType();
        if (request.commercialPlanType() != null && request.commercialPlanType() != previousPlan) {
            tenant.setCommercialPlanType(request.commercialPlanType());
            auditEventService.auditAdminAction(
                    actorUserId, tenant.getId(), AuditEventType.ADMIN_COMMERCIAL_PLAN_CHANGED,
                    "Plano comercial alterado de " + previousPlan + " para " + request.commercialPlanType(),
                    auditMetadata.of("from", previousPlan.name(), "to", request.commercialPlanType().name()));
        }

        if (request.commercialCondition() != null
                && !request.commercialCondition().equals(tenant.getCommercialCondition())) {
            tenant.setCommercialCondition(blankToNull(request.commercialCondition()));
            auditEventService.auditAdminAction(
                    actorUserId, tenant.getId(), AuditEventType.ADMIN_COMMERCIAL_CONDITION_CHANGED,
                    "Condição comercial atualizada.",
                    auditMetadata.of("commercialCondition", tenant.getCommercialCondition()));
        }

        tenant.setUpdatedAt(Instant.now());

        auditEventService.auditAdminAction(
                actorUserId, tenant.getId(), AuditEventType.ADMIN_TENANT_UPDATED,
                "Dados do cliente atualizados.",
                auditMetadata.of("tenantId", tenant.getId()));

        return toDetail(tenant, resolvePeriod(null, null));
    }

    // ------------------------------------------------------------------
    // Suspensão / reativação / cancelamento
    // ------------------------------------------------------------------

    @Transactional
    public TenantAdminDetailResponse suspend(String actorUserId, String tenantId, String reason) {
        TenantEntity tenant = requireClient(tenantId);
        TenantStatus previous = tenant.getStatus();
        tenant.setStatus(TenantStatus.SUSPENSO);
        tenant.setUpdatedAt(Instant.now());

        auditEventService.auditAdminAction(
                actorUserId, tenant.getId(), AuditEventType.ADMIN_TENANT_SUSPENDED,
                "Cliente suspenso. Motivo: " + reason,
                auditMetadata.of("reason", reason, "previousStatus", previous.name()));

        return toDetail(tenant, resolvePeriod(null, null));
    }

    @Transactional
    public TenantAdminDetailResponse reactivate(
            String actorUserId,
            String tenantId,
            ReactivateTenantAdminRequest request
    ) {
        TenantStatus target = request.targetStatus() == null ? TenantStatus.ATIVO : request.targetStatus();
        if (target != TenantStatus.ATIVO && target != TenantStatus.EM_TESTE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Reativação só aceita os status ATIVO ou EM_TESTE.");
        }

        TenantEntity tenant = requireClient(tenantId);
        TenantStatus previous = tenant.getStatus();
        tenant.setStatus(target);
        tenant.setUpdatedAt(Instant.now());

        auditEventService.auditAdminAction(
                actorUserId, tenant.getId(), AuditEventType.ADMIN_TENANT_REACTIVATED,
                "Cliente reativado para " + target + ". Motivo: " + request.reason(),
                auditMetadata.of("reason", request.reason(), "previousStatus", previous.name(),
                        "targetStatus", target.name()));

        return toDetail(tenant, resolvePeriod(null, null));
    }

    @Transactional
    public TenantAdminDetailResponse cancel(String actorUserId, String tenantId, String reason) {
        TenantEntity tenant = requireClient(tenantId);
        TenantStatus previous = tenant.getStatus();
        tenant.setStatus(TenantStatus.CANCELADO);
        tenant.setUpdatedAt(Instant.now());

        auditEventService.auditAdminAction(
                actorUserId, tenant.getId(), AuditEventType.ADMIN_TENANT_CANCELED,
                "Cliente cancelado. Motivo: " + reason,
                auditMetadata.of("reason", reason, "previousStatus", previous.name()));

        return toDetail(tenant, resolvePeriod(null, null));
    }

    // ------------------------------------------------------------------
    // Usuários do cliente
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(String tenantId) {
        requireClient(tenantId);
        return userRepository.findByTenantIdOrderByCreatedAtAsc(tenantId).stream()
                .map(AdminTenantService::toUserResponse)
                .toList();
    }

    @Transactional
    public InviteUserAdminResponse inviteUser(
            String actorUserId,
            String tenantId,
            InviteUserAdminRequest request
    ) {
        requireClient(tenantId);
        if (userRepository.existsByTenantIdAndEmail(tenantId, request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe um usuário com este e-mail neste cliente.");
        }

        InvitedUser invited = provisionResponsibleUser(
                actorUserId, tenantId, request.name(), request.email(), true, Instant.now());
        return new InviteUserAdminResponse(toUserResponse(invited.user()), invited.inviteUrl());
    }

    @Transactional
    public InviteUserAdminResponse resendInvite(String actorUserId, String tenantId, Long userId) {
        requireClient(tenantId);
        UserEntity user = userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        // Reenviar convite não pode rebaixar um usuário já ativo de volta para CONVIDADO:
        // isso reabriria o login por senha conhecida durante a janela do convite.
        if (user.getStatus() != UserStatus.CONVIDADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Só é possível reenviar convite para usuário que ainda está como CONVIDADO."
            );
        }

        String token = generateInviteToken(user);
        user.setStatus(UserStatus.CONVIDADO);

        auditEventService.auditAdminAction(
                actorUserId, tenantId, AuditEventType.ADMIN_USER_INVITE_RESENT,
                "Convite reenviado para " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail()));

        return new InviteUserAdminResponse(toUserResponse(user), inviteUrl(token));
    }

    @Transactional
    public AdminUserResponse blockUser(String actorUserId, String tenantId, Long userId) {
        UserEntity user = requireTenantUser(tenantId, userId);
        user.setStatus(UserStatus.BLOQUEADO);

        auditEventService.auditAdminAction(
                actorUserId, tenantId, AuditEventType.ADMIN_USER_BLOCKED,
                "Usuário bloqueado: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail()));

        return toUserResponse(user);
    }

    @Transactional
    public AdminUserResponse unblockUser(String actorUserId, String tenantId, Long userId) {
        UserEntity user = requireTenantUser(tenantId, userId);
        user.setStatus(UserStatus.ATIVO);

        auditEventService.auditAdminAction(
                actorUserId, tenantId, AuditEventType.ADMIN_USER_UNBLOCKED,
                "Usuário desbloqueado: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail()));

        return toUserResponse(user);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private InvitedUser provisionResponsibleUser(
            String actorUserId,
            String tenantId,
            String name,
            String email,
            boolean sendInvite,
            Instant now
    ) {
        UserEntity user = new UserEntity();
        user.setTenantId(tenantId);
        user.setName(name.trim());
        user.setEmail(email.trim());
        user.setRoles(Set.of(EMPRESA_ROLE));
        user.setCreatedAt(now);
        // Senha aleatória inutilizável: o acesso é estabelecido pelo convite.
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));

        String token = null;
        if (sendInvite) {
            token = generateInviteToken(user);
            user.setStatus(UserStatus.CONVIDADO);
        } else {
            user.setStatus(UserStatus.ATIVO);
        }
        userRepository.save(user);

        auditEventService.auditAdminAction(
                actorUserId, tenantId, AuditEventType.ADMIN_USER_INVITED,
                "Usuário EMPRESA criado: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail(),
                        "role", EMPRESA_ROLE, "sendInvite", sendInvite));

        return new InvitedUser(user, sendInvite ? inviteUrl(token) : null);
    }

    private String generateInviteToken(UserEntity user) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = URL_ENCODER.encodeToString(bytes);
        user.setInviteTokenHash(passwordEncoder.encode(token));
        user.setInvitedAt(Instant.now());
        user.setInviteExpiresAt(Instant.now().plus(inviteTtlHours, ChronoUnit.HOURS));
        return token;
    }

    private String inviteUrl(String token) {
        if (token == null) {
            return null;
        }
        return publicBaseUrl + "/convite/" + token;
    }

    private TenantEntity requireClient(String tenantId) {
        if (PLATFORM_TENANT_ID.equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado.");
        }
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
    }

    private UserEntity requireTenantUser(String tenantId, Long userId) {
        requireClient(tenantId);
        return userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private String resolveCompanyId(String requested) {
        if (requested != null && !requested.isBlank()) {
            String candidate = requested.trim();
            if (tenantRepository.findFirstByCompanyId(candidate).isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Já existe um cliente com este identificador de integração.");
            }
            return candidate;
        }
        String generated;
        do {
            generated = "emp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        } while (tenantRepository.findFirstByCompanyId(generated).isPresent());
        return generated;
    }

    private String generateTenantId() {
        String id;
        do {
            id = "tnt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        } while (tenantRepository.existsById(id));
        return id;
    }

    private TenantAdminSummaryResponse toSummary(TenantEntity tenant, Period period) {
        return new TenantAdminSummaryResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getTradeName(),
                tenant.getTaxId(),
                tenant.getCorporateEmail(),
                tenant.getCommercialPlanType(),
                tenant.getStatus(),
                adminUsageService.countCompletedInPeriod(tenant.getId(), period.start(), period.end()),
                tenant.getCreatedAt()
        );
    }

    private TenantAdminDetailResponse toDetail(TenantEntity tenant, Period period) {
        List<AdminUserResponse> users = userRepository
                .findByTenantIdOrderByCreatedAtAsc(tenant.getId()).stream()
                .map(AdminTenantService::toUserResponse)
                .toList();
        return new TenantAdminDetailResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getTradeName(),
                tenant.getLegalName(),
                tenant.getTaxId(),
                tenant.getCorporateEmail(),
                tenant.getPhone(),
                tenant.getWebsite(),
                tenant.isHealthVertical(),
                tenant.getCommercialPlanType(),
                tenant.getCommercialCondition(),
                tenant.getStatus(),
                adminUsageService.countCompletedInPeriod(tenant.getId(), period.start(), period.end()),
                users,
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }

    private static AdminUserResponse toUserResponse(UserEntity user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    private Period resolvePeriod(Instant periodStart, Instant periodEnd) {
        Instant end = periodEnd == null ? Instant.now() : periodEnd;
        Instant start = periodStart == null ? end.minus(usagePeriodDays, ChronoUnit.DAYS) : periodStart;
        return new Period(start, end);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private record Period(Instant start, Instant end) {
    }

    private record InvitedUser(UserEntity user, String inviteUrl) {
    }
}
